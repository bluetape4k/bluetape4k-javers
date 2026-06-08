package io.bluetape4k.javers.examples.exposedddd

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.codec.Base58
import io.bluetape4k.javers.ddd.DomainEventPublisher
import io.bluetape4k.javers.examples.exposedddd.domain.CustomerId
import io.bluetape4k.javers.examples.exposedddd.domain.MarkOrderPaidCommand
import io.bluetape4k.javers.examples.exposedddd.domain.Order
import io.bluetape4k.javers.examples.exposedddd.domain.OrderId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderItem
import io.bluetape4k.javers.examples.exposedddd.domain.OrderStatus
import io.bluetape4k.javers.examples.exposedddd.domain.PlaceOrderCommand
import io.bluetape4k.javers.examples.exposedddd.messaging.OrderKafkaEventPublisher
import io.bluetape4k.javers.examples.exposedddd.messaging.OrderProjectionEventConsumer
import io.bluetape4k.javers.examples.exposedddd.persistence.OrderRepository
import io.bluetape4k.javers.examples.exposedddd.persistence.OrdersTable
import io.bluetape4k.javers.examples.exposedddd.projection.OrderSummary
import io.bluetape4k.javers.examples.exposedddd.projection.RedisOrderSummaryProjection
import io.bluetape4k.javers.examples.exposedddd.service.OrderCommandHandler
import io.bluetape4k.javers.examples.exposedddd.service.OrderQueryService
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepository
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTable
import io.bluetape4k.javers.persistence.exposed.schema.CommitTable
import io.bluetape4k.testcontainers.mq.KafkaServer
import io.bluetape4k.testcontainers.storage.RedisServer
import org.javers.core.JaversBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderProjectionFlowTest {

    private val database: Database = Database.connect(
        url = "jdbc:h2:mem:javers-exposed-ddd-projection-${Base58.randomString(8)};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
    )

    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-27T00:00:00Z"), ZoneOffset.UTC)

    @BeforeEach
    fun beforeEach() {
        transaction(database) {
            SchemaUtils.drop(OrdersTable, CdoSnapshotTable, CommitTable)
            SchemaUtils.create(CommitTable, CdoSnapshotTable, OrdersTable)
        }
    }

    @Test
    fun `command events update Redis order summary through Kafka`() {
        val topic = "javers-exposed-ddd-orders-${Base58.randomString(8)}"
        val orderId = OrderId("order-projection-1")

        KafkaServer.Launcher.createStringProducer(kafka).use { producer ->
            KafkaServer.Launcher.createStringConsumer(kafka).use { consumer ->
                val redisClient = RedisServer.Launcher.LettuceLib.getRedisClient(redis.url)
                try {
                    val projection = RedisOrderSummaryProjection(
                        client = redisClient,
                        keyPrefix = "test:${Base58.randomString(8)}:order-summary",
                    )
                    val queryService = OrderQueryService(projection)
                    val projectionConsumer = OrderProjectionEventConsumer(consumer, projection).apply {
                        subscribe(topic)
                    }
                    val handler = newCommandHandler(OrderKafkaEventPublisher(producer, topic))

                    handler.handle(
                        PlaceOrderCommand(
                            orderId = orderId,
                            author = "projection-test",
                            customerId = CustomerId("customer-projection-1"),
                            items = listOf(OrderItem("sku-projection-1", quantity = 2, unitPrice = BigDecimal("15.00"))),
                        ),
                    )

                    val placed = awaitSummary(projectionConsumer, queryService, orderId, OrderStatus.PLACED)
                    placed.customerId shouldBeEqualTo CustomerId("customer-projection-1")
                    placed.totalAmount shouldBeEqualTo BigDecimal("30.00")

                    handler.handle(MarkOrderPaidCommand(orderId = orderId, author = "projection-test"))

                    val paid = awaitSummary(projectionConsumer, queryService, orderId, OrderStatus.PAID)
                    paid.status shouldBeEqualTo OrderStatus.PAID
                    paid.lastUpdated shouldBeEqualTo clock.instant()
                } finally {
                    redisClient.shutdown()
                }
            }
        }
    }

    private fun newCommandHandler(eventPublisher: DomainEventPublisher): OrderCommandHandler {
        val snapshotRepository = ExposedCdoSnapshotRepository(database)
        val javers = JaversBuilder.javers()
            .registerJaversRepository(snapshotRepository)
            .registerEntity(Order::class.java)
            .build()
        val repository = OrderRepository(database, javers, eventPublisher)
        return OrderCommandHandler(repository, clock)
    }

    private fun awaitSummary(
        projectionConsumer: OrderProjectionEventConsumer,
        queryService: OrderQueryService,
        orderId: OrderId,
        status: OrderStatus,
    ): OrderSummary {
        repeat(20) {
            projectionConsumer.pollOnce(Duration.ofMillis(250))
            val summary = queryService.findSummary(orderId)
            if (summary?.status == status) {
                return summary
            }
        }
        error("Timed out waiting for order summary status=$status")
    }

    companion object {
        private val kafka = KafkaServer.Launcher.kafka
        private val redis = RedisServer.Launcher.redis
    }
}
