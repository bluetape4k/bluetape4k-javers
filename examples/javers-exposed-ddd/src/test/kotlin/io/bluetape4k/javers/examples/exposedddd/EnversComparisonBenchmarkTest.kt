package io.bluetape4k.javers.examples.exposedddd

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.javers.examples.exposedddd.domain.CustomerId
import io.bluetape4k.javers.examples.exposedddd.domain.Order
import io.bluetape4k.javers.examples.exposedddd.domain.OrderId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderItem
import io.bluetape4k.javers.examples.exposedddd.domain.OrderPlaced
import io.bluetape4k.javers.examples.exposedddd.domain.OrderStatus
import io.bluetape4k.javers.examples.exposedddd.persistence.OrderRepository
import io.bluetape4k.javers.examples.exposedddd.persistence.OrdersTable
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepository
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTable
import io.bluetape4k.javers.persistence.exposed.schema.CommitTable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.Audited
import org.hibernate.SessionFactory
import org.javers.core.JaversBuilder
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
import java.util.UUID
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
        val javersExposed = measureJaversExposed(warmupCount, measuredCount)
        val artifact = writeArtifact(warmupCount, measuredCount, envers, javersExposed)

        Files.exists(artifact).shouldBeTrue()
        (envers.results + javersExposed.results).all { it.millisPerOperation > 0.0 }.shouldBeTrue()
    }

    private fun measureEnvers(warmupCount: Int, measuredCount: Int): ImplementationResult {
        return newSessionFactory().use { sessionFactory ->
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

    private fun measureJaversExposed(warmupCount: Int, measuredCount: Int): ImplementationResult {
        val database = Database.connect(
            url = "jdbc:h2:mem:javers-benchmark-${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
        transaction(database) {
            SchemaUtils.create(CommitTable, CdoSnapshotTable, OrdersTable)
        }
        val snapshotRepository = ExposedCdoSnapshotRepository(database)
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

        return ImplementationResult("JaVers + Exposed", listOf(insert, update, auditQuery))
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
            AuditReaderFactory.get(session).getRevisions(EnversOrderEntity::class.java, id)
        }
    }

    private fun saveJavers(repository: OrderRepository, id: String) {
        val order = Order.place(
            id = OrderId(id),
            customerId = CustomerId("customer-$id"),
            items = listOf(OrderItem("sku-$id", quantity = 2, unitPrice = BigDecimal("12.50"))),
            now = NOW,
        )
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

    private fun newSessionFactory(): SessionFactory {
        val enversUrl = "jdbc:h2:mem:envers-benchmark-${UUID.randomUUID()};DB_CLOSE_DELAY=-1"
        val registry = StandardServiceRegistryBuilder()
            .applySettings(
                Properties().apply {
                    put("hibernate.connection.driver_class", "org.h2.Driver")
                    put("hibernate.connection.url", enversUrl)
                    put("hibernate.connection.username", "sa")
                    put("hibernate.connection.password", "")
                    put("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
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
        javersExposed: ImplementationResult,
    ): Path {
        val artifact = Path.of("../..", "docs/benchmark/2026-05-27-javers-exposed-ddd-envers-comparison.json")
            .normalize()
        artifact.parent.createDirectories()
        artifact.writeText(
            JsonObject().apply {
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
                })
                add("implementations", JsonArray().apply {
                    add(envers.toJson())
                    add(javersExposed.toJson())
                })
            }.toString(),
        )
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
        private val NOW: Instant = Instant.parse("2026-05-27T00:00:00Z")
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
