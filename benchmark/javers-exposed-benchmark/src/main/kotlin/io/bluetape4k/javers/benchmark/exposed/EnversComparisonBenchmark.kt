package io.bluetape4k.javers.benchmark.exposed

import com.zaxxer.hikari.HikariDataSource
import io.bluetape4k.codec.Base58
import io.bluetape4k.javers.examples.exposedddd.domain.CustomerId
import io.bluetape4k.javers.examples.exposedddd.domain.Order
import io.bluetape4k.javers.examples.exposedddd.domain.OrderId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderItem
import io.bluetape4k.javers.examples.exposedddd.domain.OrderPlaced
import io.bluetape4k.javers.examples.exposedddd.domain.OrderStatus
import io.bluetape4k.javers.examples.exposedddd.persistence.OrderRepository
import io.bluetape4k.javers.examples.exposedddd.persistence.OrdersTable
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepository
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepositoryOptions
import io.bluetape4k.javers.persistence.exposed.schema.ExposedJaversTableNames
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import io.bluetape4k.testcontainers.database.getHikariDataSource
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown
import org.hibernate.SessionFactory
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.Audited
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.repository.inmemory.InMemoryRepository
import org.javers.repository.jql.QueryBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.infra.BenchmarkParams
import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.util.Properties
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

/**
 * Compares bounded audit workflow costs across Hibernate Envers and JaVers paths.
 *
 * This benchmark intentionally lives in the benchmark module so ordinary example tests
 * stay behavior-focused and do not rewrite benchmark evidence as a side effect.
 */
@State(Scope.Benchmark)
open class EnversComparisonBenchmark {

    @Param("envers", "javers_in_memory", "javers_exposed_repository", "javers_exposed_ddd")
    lateinit var implementationName: String

    private lateinit var implementation: AuditBenchmarkImplementation

    private val insertCounter = AtomicInteger()
    private val updateCounter = AtomicInteger()
    private val updateTargetCounter = AtomicInteger()
    private val auditQueryCounter = AtomicInteger()
    private lateinit var updateTargetId: String

    @Setup(Level.Trial)
    fun setup() {
        implementation = AuditImplementationVariant.from(implementationName).create()
        repeat(CORPUS_SIZE) { index ->
            implementation.insert("corpus-$index")
        }
        implementation.afterCorpusLoaded()
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        if (::implementation.isInitialized) {
            implementation.close()
        }
    }

    @Setup(Level.Invocation)
    fun setupInvocation(params: BenchmarkParams) {
        if (params.benchmark.endsWith(".update")) {
            updateTargetId = "update-${updateTargetCounter.getAndIncrement()}"
            implementation.insert(updateTargetId)
        }
    }

    @Benchmark
    fun insert(): String {
        val id = "insert-${insertCounter.getAndIncrement()}"
        return implementation.insert(id)
    }

    @Benchmark
    fun update(): String {
        val index = updateCounter.getAndIncrement()
        return implementation.update(updateTargetId, NOW.plusSeconds(index.toLong() + 1))
    }

    @Benchmark
    fun auditQuery(): Int {
        val id = "corpus-${auditQueryCounter.getAndIncrement() % CORPUS_SIZE}"
        return implementation.auditQuery(id)
    }

    private interface AuditBenchmarkImplementation: AutoCloseable {
        fun insert(id: String): String

        fun update(id: String, updatedAt: Instant): String

        fun auditQuery(id: String): Int

        fun afterCorpusLoaded() = Unit
    }

    private enum class AuditImplementationVariant(
        val paramName: String,
        val create: () -> AuditBenchmarkImplementation,
    ) {
        Envers("envers", { EnversImplementation() }),
        JaversInMemory("javers_in_memory", { JaversInMemoryImplementation() }),
        JaversExposedRepository("javers_exposed_repository", { JaversExposedRepositoryImplementation() }),
        JaversExposedDdd("javers_exposed_ddd", { JaversExposedDddImplementation() });

        companion object {
            fun from(paramName: String): AuditImplementationVariant {
                return entries.single { it.paramName == paramName }
            }
        }
    }

    private class EnversImplementation: AuditBenchmarkImplementation {
        private val dataSource = newBenchmarkDataSource("envers")
        private val sessionFactory = newSessionFactory(dataSource)

        override fun insert(id: String): String {
            sessionFactory.openSession().use { session ->
                val transaction = session.beginTransaction()
                session.persist(EnversOrderEntity(id = id))
                transaction.commit()
            }
            return id
        }

        override fun update(id: String, updatedAt: Instant): String {
            sessionFactory.openSession().use { session ->
                val transaction = session.beginTransaction()
                val entity = requireNotNull(session.find(EnversOrderEntity::class.java, id))
                entity.status = OrderStatus.PAID.name
                entity.updatedAt = updatedAt.toString()
                transaction.commit()
            }
            return id
        }

        override fun auditQuery(id: String): Int {
            var revisions = 0
            sessionFactory.openSession().use { session ->
                val auditReader = AuditReaderFactory.get(session)
                auditReader.getRevisions(EnversOrderEntity::class.java, id).forEach { revision ->
                    auditReader.find(EnversOrderEntity::class.java, id, revision)
                    revisions++
                }
            }
            return revisions
        }

        override fun close() {
            sessionFactory.close()
            dataSource.close()
        }
    }

    private class JaversInMemoryImplementation: AuditBenchmarkImplementation {
        private val javers: Javers = JaversBuilder.javers()
            .registerJaversRepository(InMemoryRepository())
            .registerEntity(Order::class.java)
            .build()

        override fun insert(id: String): String {
            return javers.commit("benchmark", placedOrder(id)).id.value()
        }

        override fun update(id: String, updatedAt: Instant): String {
            val order = placedOrder(id).markPaid(updatedAt)
            return javers.commit("benchmark", order).id.value()
        }

        override fun auditQuery(id: String): Int {
            return javers
                .findSnapshots(QueryBuilder.byInstanceId(OrderId(id), Order::class.java).build())
                .size
        }

        override fun close() = Unit
    }

    private class JaversExposedRepositoryImplementation: AuditBenchmarkImplementation {
        private val dataSource = newBenchmarkDataSource("repository")
        private val database = Database.connect(dataSource)
        private val options = newRepositoryOptions("repo")
        private val snapshotRepository: ExposedCdoSnapshotRepository
        private val javers: Javers

        init {
            val schema = options.newSchema()
            transaction(database) {
                SchemaUtils.create(*schema.tables)
            }
            snapshotRepository = ExposedCdoSnapshotRepository(database, options = options)
            javers = JaversBuilder.javers()
                .registerJaversRepository(snapshotRepository)
                .registerEntity(Order::class.java)
                .build()
        }

        override fun insert(id: String): String {
            return javers.commit("benchmark", placedOrder(id)).id.value()
        }

        override fun update(id: String, updatedAt: Instant): String {
            val order = placedOrder(id).markPaid(updatedAt)
            return javers.commit("benchmark", order).id.value()
        }

        override fun auditQuery(id: String): Int {
            return javers
                .findSnapshots(QueryBuilder.byInstanceId(OrderId(id), Order::class.java).build())
                .size
        }

        override fun close() {
            runCatching {
                transaction(database) {
                    SchemaUtils.drop(*options.newSchema().tables)
                }
            }
            dataSource.close()
        }
    }

    private class JaversExposedDddImplementation: AuditBenchmarkImplementation {
        private val dataSource = newBenchmarkDataSource("ddd")
        private val database = Database.connect(dataSource)
        private val options = newRepositoryOptions("ddd")
        private val snapshotRepository: ExposedCdoSnapshotRepository
        private val repository: OrderRepository

        init {
            val schema = options.newSchema()
            transaction(database) {
                SchemaUtils.create(*schema.tables, OrdersTable)
            }
            snapshotRepository = ExposedCdoSnapshotRepository(database, options = options)
            val javers = JaversBuilder.javers()
                .registerJaversRepository(snapshotRepository)
                .registerEntity(Order::class.java)
                .build()
            repository = OrderRepository(database, javers)
        }

        override fun insert(id: String): String {
            val order = placedOrder(id)
            repository.save(
                aggregate = order,
                author = "benchmark",
                event = OrderPlaced(
                    aggregateId = order.id,
                    occurredOn = NOW,
                    customerId = order.customerId,
                    totalAmount = order.totalAmount.toPlainString(),
                ),
            )
            return id
        }

        override fun update(id: String, updatedAt: Instant): String {
            val current = requireNotNull(repository.load(OrderId(id)))
            repository.save(
                aggregate = current.markPaid(updatedAt),
                author = "benchmark",
            )
            return id
        }

        override fun auditQuery(id: String): Int {
            return repository.loadHistory(OrderId(id)).size
        }

        override fun close() {
            runCatching {
                transaction(database) {
                    SchemaUtils.drop(OrdersTable)
                    SchemaUtils.drop(*options.newSchema().tables)
                }
            }
            dataSource.close()
        }
    }

    companion object {
        private val postgres: PostgreSQLServer by lazy { PostgreSQLServer.Launcher.postgres }
        private val NOW: Instant = Instant.parse("2026-06-08T00:00:00Z")
        private const val CORPUS_SIZE = 40

        private fun newBenchmarkDataSource(name: String): HikariDataSource {
            return postgres.getHikariDataSource {
                poolName = "javers-envers-$name-${Base58.randomString(6)}"
                maximumPoolSize = 4
                minimumIdle = 1
            }
        }

        private fun newRepositoryOptions(prefix: String): ExposedCdoSnapshotRepositoryOptions {
            val suffix = Base58.randomString(6).lowercase()
            return ExposedCdoSnapshotRepositoryOptions(
                tableNames = ExposedJaversTableNames(
                    commitTableName = "javers_${prefix}_commit_$suffix",
                    snapshotTableName = "javers_${prefix}_snapshot_$suffix",
                ),
            )
        }

        private fun newSessionFactory(dataSource: DataSource): SessionFactory {
            val registry = StandardServiceRegistryBuilder()
                .applySettings(
                    Properties().apply {
                        put("hibernate.connection.datasource", dataSource)
                        put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                        put("hibernate.hbm2ddl.auto", "create-drop")
                        put("hibernate.show_sql", "false")
                        put("hibernate.format_sql", "false")
                        put("jakarta.persistence.validation.mode", "none")
                    },
                )
                .build()
            return MetadataSources(registry)
                .addAnnotatedClass(EnversOrderEntity::class.java)
                .buildMetadata()
                .buildSessionFactory()
        }

        private fun placedOrder(id: String): Order {
            return Order.place(
                id = OrderId(id),
                customerId = CustomerId("customer-$id"),
                items = listOf(OrderItem("sku-$id", quantity = 2, unitPrice = BigDecimal("12.50"))),
                now = NOW,
            )
        }
    }
}

@Entity(name = "EnversOrder")
@Table(name = "envers_order")
@Audited
internal open class EnversOrderEntity(
    @Id
    @Column(name = "id", nullable = false, length = 64)
    open var id: String = "",

    @Column(name = "customer_id", nullable = false, length = 64)
    open var customerId: String = "customer",

    @Column(name = "status", nullable = false, length = 32)
    open var status: String = OrderStatus.PLACED.name,

    @Column(name = "total_amount", nullable = false, length = 32)
    open var totalAmount: String = "25.00",

    @Column(name = "created_at", nullable = false, length = 40)
    open var createdAt: String = "2026-05-27T00:00:00Z",

    @Column(name = "updated_at", nullable = false, length = 40)
    open var updatedAt: String = "2026-05-27T00:00:00Z",
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
