package io.bluetape4k.javers.ddd

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepository
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTable
import io.bluetape4k.javers.persistence.exposed.schema.CommitTable
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.annotation.Id
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class AggregateRepositoryTest {

    private val database: Database = Database.connect(
        url = "jdbc:h2:mem:javers-ddd-${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
    )

    private lateinit var javers: Javers
    private lateinit var publishedEvents: MutableList<DomainEvent>

    @BeforeEach
    fun beforeEach() {
        transaction(database) {
            SchemaUtils.drop(CdoSnapshotTable, CommitTable)
            SchemaUtils.create(CommitTable, CdoSnapshotTable)
        }
        val repository = ExposedCdoSnapshotRepository(database)
        javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .registerEntity(Order::class.java)
            .build()
        publishedEvents = mutableListOf()
    }

    @Test
    fun `save persists aggregate commits JaVers snapshot and publishes event`() {
        val repository = OrderRepository(javers, FunctionDomainEventPublisher { publishedEvents += it })
        val aggregate = Order(id = 1L, status = "PLACED")
        val event = OrderPlaced(aggregateId = aggregate.id)

        val saved = repository.save(aggregate, author = "tester", event = event)

        saved shouldBeEqualTo aggregate
        repository.load(aggregate.id).shouldNotBeNull() shouldBeEqualTo aggregate
        repository.loadHistory(aggregate.id).single().commitMetadata.properties[DOMAIN_EVENT_TYPE_PROPERTY] shouldBeEqualTo
            OrderPlaced::class.qualifiedName
        publishedEvents.single() shouldBeEqualTo event
    }

    @Test
    fun `load falls back to latest JaVers shadow when store misses`() {
        val repository = OrderRepository(javers)
        val aggregate = Order(id = 2L, status = "PLACED")
        repository.save(aggregate, author = "tester")
        aggregate.status = "PAID"
        repository.save(aggregate, author = "tester")

        repository.clear()

        val loaded = repository.load(aggregate.id)

        loaded.shouldNotBeNull()
        loaded.id shouldBeEqualTo aggregate.id
        loaded.status shouldBeEqualTo "PAID"
        repository.loadHistory(aggregate.id).size shouldBeEqualTo 2
    }

    data class Order(
        @Id
        override val id: Long,
        var status: String,
    ): AggregateRoot<Long>

    data class OrderPlaced(
        override val aggregateId: Long,
        override val occurredOn: Instant = Instant.parse("2026-05-26T00:00:00Z"),
    ): DomainEvent

    private class OrderRepository(
        javers: Javers,
        eventPublisher: DomainEventPublisher = NoopDomainEventPublisher,
    ): AggregateRepository<Order, Long>(Order::class.java, javers, eventPublisher) {

        private val orders = linkedMapOf<Long, Order>()

        override fun persist(aggregate: Order): Order {
            val saved = aggregate.copy()
            orders[saved.id] = saved
            return saved
        }

        override fun findById(id: Long): Order? = orders[id]?.copy()

        fun clear() {
            orders.clear()
        }
    }
}
