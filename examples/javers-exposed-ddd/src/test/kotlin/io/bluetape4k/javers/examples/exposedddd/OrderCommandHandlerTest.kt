package io.bluetape4k.javers.examples.exposedddd

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.javers.ddd.DOMAIN_EVENT_TYPE_PROPERTY
import io.bluetape4k.javers.ddd.DomainEvent
import io.bluetape4k.javers.ddd.FunctionDomainEventPublisher
import io.bluetape4k.javers.examples.exposedddd.domain.CustomerId
import io.bluetape4k.javers.examples.exposedddd.domain.MarkOrderPaidCommand
import io.bluetape4k.javers.examples.exposedddd.domain.Order
import io.bluetape4k.javers.examples.exposedddd.domain.OrderId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderItem
import io.bluetape4k.javers.examples.exposedddd.domain.OrderMarkedPaid
import io.bluetape4k.javers.examples.exposedddd.domain.OrderPlaced
import io.bluetape4k.javers.examples.exposedddd.domain.OrderStatus
import io.bluetape4k.javers.examples.exposedddd.domain.PlaceOrderCommand
import io.bluetape4k.javers.examples.exposedddd.persistence.OrderRepository
import io.bluetape4k.javers.examples.exposedddd.persistence.OrdersTable
import io.bluetape4k.javers.examples.exposedddd.service.OrderCommandHandler
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepository
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTable
import io.bluetape4k.javers.persistence.exposed.schema.CommitTable
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class OrderCommandHandlerTest {

    private val database: Database = Database.connect(
        url = "jdbc:h2:mem:javers-exposed-ddd-${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
    )

    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-26T00:00:00Z"), ZoneOffset.UTC)

    private lateinit var javers: Javers
    private lateinit var repository: OrderRepository
    private lateinit var handler: OrderCommandHandler
    private lateinit var publishedEvents: MutableList<DomainEvent>

    @BeforeEach
    fun beforeEach() {
        transaction(database) {
            SchemaUtils.drop(OrdersTable, CdoSnapshotTable, CommitTable)
            SchemaUtils.create(CommitTable, CdoSnapshotTable, OrdersTable)
        }

        val snapshotRepository = ExposedCdoSnapshotRepository(database)
        javers = JaversBuilder.javers()
            .registerJaversRepository(snapshotRepository)
            .registerEntity(Order::class.java)
            .build()

        publishedEvents = mutableListOf()
        repository = OrderRepository(
            database = database,
            javers = javers,
            eventPublisher = FunctionDomainEventPublisher { publishedEvents += it },
        )
        handler = OrderCommandHandler(repository, clock)
    }

    @Test
    fun `place order persists aggregate commits snapshot and publishes event`() {
        val command = PlaceOrderCommand(
            orderId = OrderId("order-1"),
            author = "tester",
            customerId = CustomerId("customer-1"),
            items = listOf(OrderItem("sku-1", quantity = 2, unitPrice = BigDecimal("12.50"))),
        )

        val saved = handler.handle(command)

        saved.status shouldBeEqualTo OrderStatus.PLACED
        saved.totalAmount shouldBeEqualTo BigDecimal("25.00")
        repository.load(command.orderId).shouldNotBeNull() shouldBeEqualTo saved
        repository.loadHistory(command.orderId).single().commitMetadata.properties[DOMAIN_EVENT_TYPE_PROPERTY] shouldBeEqualTo
            OrderPlaced::class.qualifiedName
        publishedEvents.single() shouldBeEqualTo OrderPlaced(
            aggregateId = command.orderId,
            occurredOn = clock.instant(),
            customerId = command.customerId,
            totalAmount = "25.00",
        )
    }

    @Test
    fun `mark order paid creates second snapshot and publishes paid event`() {
        val orderId = OrderId("order-2")
        handler.handle(
            PlaceOrderCommand(
                orderId = orderId,
                author = "tester",
                customerId = CustomerId("customer-2"),
                items = listOf(OrderItem("sku-2", quantity = 1, unitPrice = BigDecimal("30.00"))),
            ),
        )

        val paid = handler.handle(MarkOrderPaidCommand(orderId = orderId, author = "tester"))

        paid.status shouldBeEqualTo OrderStatus.PAID
        repository.load(orderId).shouldNotBeNull().status shouldBeEqualTo OrderStatus.PAID
        repository.loadHistory(orderId).size shouldBeEqualTo 2
        publishedEvents.map { it::class } shouldBeEqualTo listOf(OrderPlaced::class, OrderMarkedPaid::class)
    }
}
