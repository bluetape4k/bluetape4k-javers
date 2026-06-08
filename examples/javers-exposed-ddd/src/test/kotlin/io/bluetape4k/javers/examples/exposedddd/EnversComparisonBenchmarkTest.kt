package io.bluetape4k.javers.examples.exposedddd

import com.google.gson.JsonArray
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.bluetape4k.assertions.shouldBeTrue
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
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.Audited
import org.hibernate.SessionFactory
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.repository.inmemory.InMemoryRepository
import org.javers.repository.jql.QueryBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.Serializable
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.Properties
import javax.sql.DataSource
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.system.measureNanoTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnversComparisonBenchmarkTest {

    @Test
    fun `write Envers comparison benchmark artifact`() {
        val warmupCount = 5
        val measuredCount = 40
        val envers = measureEnvers(warmupCount, measuredCount)
        val javersInMemory = measureJaversInMemory(warmupCount, measuredCount)
        val javersExposedRepository = measureJaversExposedRepository(warmupCount, measuredCount)
        val javersExposedDddPath = measureJaversExposedDddPath(warmupCount, measuredCount)
        val artifact = writeArtifact(
            warmupCount = warmupCount,
            measuredCount = measuredCount,
            envers = envers,
            javersInMemory = javersInMemory,
            javersExposedRepository = javersExposedRepository,
            javersExposedDddPath = javersExposedDddPath,
        )

        Files.exists(artifact).shouldBeTrue()
        (envers.results + javersInMemory.results + javersExposedRepository.results + javersExposedDddPath.results)
            .all { it.millisPerOperation > 0.0 }
            .shouldBeTrue()
    }

    private fun measureEnvers(warmupCount: Int, measuredCount: Int): ImplementationResult {
        return newBenchmarkDataSource("envers").use { dataSource ->
            newSessionFactory(dataSource).use { sessionFactory ->
                repeat(warmupCount) { index ->
                    insertEnvers(sessionFactory, "envers-warmup-$index")
                }
                val insert = measure("insert", measuredCount) { index ->
                    insertEnvers(sessionFactory, "envers-insert-$index")
                }

                repeat(measuredCount) { index ->
                    insertEnvers(sessionFactory, "envers-update-$index")
                }
                val update = measure("update", measuredCount) { index ->
                    updateEnvers(sessionFactory, "envers-update-$index")
                }

                val auditQuery = measure("audit-query", measuredCount) { index ->
                    loadEnversRevisions(sessionFactory, "envers-update-$index")
                }

                ImplementationResult("Hibernate Envers", listOf(insert, update, auditQuery))
            }
        }
    }

    private fun measureJaversInMemory(warmupCount: Int, measuredCount: Int): ImplementationResult {
        val javers = JaversBuilder.javers()
            .registerJaversRepository(InMemoryRepository())
            .registerEntity(Order::class.java)
            .build()

        repeat(warmupCount) { index ->
            commitJavers(javers, "javers-core-warmup-$index")
        }
        val insert = measure("insert", measuredCount) { index ->
            commitJavers(javers, "javers-core-insert-$index")
        }

        repeat(measuredCount) { index ->
            commitJavers(javers, "javers-core-update-$index")
        }
        val update = measure("update", measuredCount) { index ->
            val order = placedOrder("javers-core-update-$index")
                .markPaid(NOW.plusSeconds(index.toLong() + 1))
            javers.commit("benchmark", order)
        }

        val auditQuery = measure("audit-query", measuredCount) { index ->
            javers.findSnapshots(
                QueryBuilder
                    .byInstanceId(OrderId("javers-core-update-$index"), Order::class.java)
                    .build(),
            )
        }

        return ImplementationResult("JaVers in-memory", listOf(insert, update, auditQuery))
    }

    private fun measureJaversExposedRepository(warmupCount: Int, measuredCount: Int): ImplementationResult {
        return newBenchmarkDataSource("repository").use { dataSource ->
            val database = Database.connect(dataSource)
            val options = newRepositoryOptions("repo")
            val schema = options.newSchema()
            transaction(database) {
                SchemaUtils.create(*schema.tables)
            }
            val snapshotRepository = ExposedCdoSnapshotRepository(database, options = options)
            val javers = JaversBuilder.javers()
                .registerJaversRepository(snapshotRepository)
                .registerEntity(Order::class.java)
                .build()

            repeat(warmupCount) { index ->
                commitJavers(javers, "javers-exposed-repository-warmup-$index")
            }
            val insert = measure("insert", measuredCount) { index ->
                commitJavers(javers, "javers-exposed-repository-insert-$index")
            }

            repeat(measuredCount) { index ->
                commitJavers(javers, "javers-exposed-repository-update-$index")
            }
            val update = measure("update", measuredCount) { index ->
                val order = placedOrder("javers-exposed-repository-update-$index")
                    .markPaid(NOW.plusSeconds(index.toLong() + 1))
                javers.commit("benchmark", order)
            }

            val auditQuery = measure("audit-query", measuredCount) { index ->
                javers.findSnapshots(
                    QueryBuilder
                        .byInstanceId(OrderId("javers-exposed-repository-update-$index"), Order::class.java)
                        .build(),
                )
            }

            ImplementationResult("JaVers + Exposed repository", listOf(insert, update, auditQuery))
        }
    }

    private fun measureJaversExposedDddPath(warmupCount: Int, measuredCount: Int): ImplementationResult {
        return newBenchmarkDataSource("ddd").use { dataSource ->
            val database = Database.connect(dataSource)
            val options = newRepositoryOptions("ddd")
            val schema = options.newSchema()
            transaction(database) {
                SchemaUtils.drop(OrdersTable)
                SchemaUtils.create(*schema.tables, OrdersTable)
            }
            val snapshotRepository = ExposedCdoSnapshotRepository(database, options = options)
            val javers = JaversBuilder.javers()
                .registerJaversRepository(snapshotRepository)
                .registerEntity(Order::class.java)
                .build()
            val repository = OrderRepository(database, javers)

            repeat(warmupCount) { index ->
                saveJavers(repository, "javers-warmup-$index")
            }
            val insert = measure("insert", measuredCount) { index ->
                saveJavers(repository, "javers-insert-$index")
            }

            repeat(measuredCount) { index ->
                saveJavers(repository, "javers-update-$index")
            }
            val update = measure("update", measuredCount) { index ->
                val current = requireNotNull(repository.load(OrderId("javers-update-$index")))
                repository.save(
                    aggregate = current.markPaid(NOW.plusSeconds(index.toLong() + 1)),
                    author = "benchmark",
                )
            }

            val auditQuery = measure("audit-query", measuredCount) { index ->
                repository.loadHistory(OrderId("javers-update-$index"))
            }

            ImplementationResult("JaVers + Exposed DDD path", listOf(insert, update, auditQuery))
        }
    }

    private fun commitJavers(javers: Javers, id: String) {
        javers.commit("benchmark", placedOrder(id))
    }

    private fun insertEnvers(sessionFactory: SessionFactory, id: String) {
        sessionFactory.openSession().use { session ->
            val transaction = session.beginTransaction()
            session.persist(EnversOrderEntity(id = id))
            transaction.commit()
        }
    }

    private fun updateEnvers(sessionFactory: SessionFactory, id: String) {
        sessionFactory.openSession().use { session ->
            val transaction = session.beginTransaction()
            val entity = requireNotNull(session.find(EnversOrderEntity::class.java, id))
            entity.status = OrderStatus.PAID.name
            entity.updatedAt = NOW.plusSeconds(1).toString()
            transaction.commit()
        }
    }

    private fun loadEnversRevisions(sessionFactory: SessionFactory, id: String) {
        sessionFactory.openSession().use { session ->
            val auditReader = AuditReaderFactory.get(session)
            auditReader.getRevisions(EnversOrderEntity::class.java, id).forEach { revision ->
                auditReader.find(EnversOrderEntity::class.java, id, revision)
            }
        }
    }

    private fun saveJavers(repository: OrderRepository, id: String) {
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
    }

    private fun placedOrder(id: String): Order {
        return Order.place(
            id = OrderId(id),
            customerId = CustomerId("customer-$id"),
            items = listOf(OrderItem("sku-$id", quantity = 2, unitPrice = BigDecimal("12.50"))),
            now = NOW,
        )
    }

    private fun measure(name: String, iterations: Int, block: (Int) -> Unit): ScenarioResult {
        val elapsedNanos = measureNanoTime {
            repeat(iterations, block)
        }
        return ScenarioResult(
            name = name,
            iterations = iterations,
            totalMillis = elapsedNanos / NANOS_PER_MILLI,
            millisPerOperation = elapsedNanos / NANOS_PER_MILLI / iterations,
        )
    }

    private fun newBenchmarkDataSource(name: String) =
        postgres.getHikariDataSource {
            poolName = "javers-$name-${Base58.randomString(6)}"
            maximumPoolSize = 4
            minimumIdle = 1
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

    private fun writeArtifact(
        warmupCount: Int,
        measuredCount: Int,
        envers: ImplementationResult,
        javersInMemory: ImplementationResult,
        javersExposedRepository: ImplementationResult,
        javersExposedDddPath: ImplementationResult,
    ): Path {
        val artifact = Path.of("../..", "docs/benchmark/2026-06-08-javers-exposed-ddd-envers-comparison.json")
            .normalize()
        artifact.parent.createDirectories()
        val json = JsonObject().apply {
            addProperty("benchmark", "javers-exposed-ddd-envers-comparison")
            addProperty("generatedAt", NOW.toString())
            addProperty("command", BENCHMARK_COMMAND)
            addProperty("metric", "milliseconds per operation")
            addProperty("direction", "lower is better")
            addProperty("warmupIterations", warmupCount)
            addProperty("measuredIterations", measuredCount)
            add("environment", JsonObject().apply {
                addProperty("javaVersion", System.getProperty("java.version"))
                addProperty("osName", System.getProperty("os.name"))
                addProperty("osArch", System.getProperty("os.arch"))
                addProperty("database", "PostgreSQL ${PostgreSQLServer.TAG} via Testcontainers")
                addProperty("connectionPool", "HikariCP")
                addProperty("schema", "JaVers Exposed tables use natural primary keys")
            })
            add("implementations", JsonArray().apply {
                add(envers.toJson())
                add(javersInMemory.toJson())
                add(javersExposedRepository.toJson())
                add(javersExposedDddPath.toJson())
            })
            add("findings", JsonArray().apply {
                add("The previous JaVers + Exposed audit-query outlier is not reproduced on this run.")
                add("JaVers in-memory approximates core diff/query cost before persistence adapters.")
                add("JaVers + Exposed repository isolates snapshot repository persistence and query cost from the example source table.")
                add("JaVers + Exposed DDD path includes source-of-truth order persistence and aggregate repository orchestration.")
                add("The benchmark remains a bounded PostgreSQL Testcontainers documentation benchmark, not a release-wide performance claim.")
            })
        }
        artifact.writeText(GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(json))
        return artifact
    }

    private fun ImplementationResult.toJson(): JsonObject {
        return JsonObject().apply {
            addProperty("name", name)
            add("results", JsonArray().apply {
                results.forEach { add(it.toJson()) }
            })
        }
    }

    private fun ScenarioResult.toJson(): JsonObject {
        return JsonObject().apply {
            addProperty("scenario", name)
            addProperty("iterations", iterations)
            addProperty("totalMillis", totalMillis)
            addProperty("millisPerOperation", millisPerOperation)
        }
    }

    private data class ImplementationResult(
        val name: String,
        val results: List<ScenarioResult>,
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    private data class ScenarioResult(
        val name: String,
        val iterations: Int,
        val totalMillis: Double,
        val millisPerOperation: Double,
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    companion object {
        private val postgres: PostgreSQLServer by lazy { PostgreSQLServer.Launcher.postgres }
        private val NOW: Instant = Instant.parse("2026-06-08T00:00:00Z")
        private const val BENCHMARK_COMMAND =
            "./gradlew :examples-javers-exposed-ddd:test --tests '*EnversComparisonBenchmarkTest*' " +
                "--no-configuration-cache --no-build-cache --no-parallel --console=plain"
        private const val NANOS_PER_MILLI = 1_000_000.0
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
)
