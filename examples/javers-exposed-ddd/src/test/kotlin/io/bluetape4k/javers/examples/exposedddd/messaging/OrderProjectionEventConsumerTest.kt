package io.bluetape4k.javers.examples.exposedddd.messaging

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.javers.examples.exposedddd.domain.CustomerId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderPlaced
import io.bluetape4k.javers.examples.exposedddd.projection.RedisOrderSummaryProjection
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

class OrderProjectionEventConsumerTest {

    @Test
    fun `successful batch commits only after projection has applied every record`() {
        val consumer = mockk<KafkaConsumer<String, String>>()
        val projection = mockk<RedisOrderSummaryProjection>()
        val event = testEvent()
        val record = record(event)
        every { consumer.poll(any()) } returns ConsumerRecords(records(record))
        every { projection.apply(event) } just Runs
        every { consumer.commitSync() } just Runs

        val processed = OrderProjectionEventConsumer(consumer, projection).pollOnce(Duration.ofMillis(1))

        processed shouldBeEqualTo 1
        verify(exactly = 1) { projection.apply(event) }
        verify(exactly = 1) { consumer.commitSync() }
    }

    @Test
    fun `failed batch does not commit and a restarted consumer can retry it`() {
        val failedConsumer = mockk<KafkaConsumer<String, String>>()
        val failedProjection = mockk<RedisOrderSummaryProjection>()
        val event = testEvent()
        val record = record(event)
        val failure = IllegalStateException("projection unavailable")
        every { failedConsumer.poll(any()) } returns ConsumerRecords(records(record))
        every { failedProjection.apply(event) } throws failure

        assertFailsWith<IllegalStateException> {
            OrderProjectionEventConsumer(failedConsumer, failedProjection).pollOnce(Duration.ofMillis(1))
        }.message shouldBeEqualTo "projection unavailable"
        verify(exactly = 0) { failedConsumer.commitSync() }

        val restartedConsumer = mockk<KafkaConsumer<String, String>>()
        val restartedProjection = mockk<RedisOrderSummaryProjection>()
        every { restartedConsumer.poll(any()) } returns ConsumerRecords(records(record))
        every { restartedProjection.apply(event) } just Runs
        every { restartedConsumer.commitSync() } just Runs

        OrderProjectionEventConsumer(restartedConsumer, restartedProjection)
            .pollOnce(Duration.ofMillis(1)) shouldBeEqualTo 1
        verify(exactly = 1) { restartedProjection.apply(event) }
        verify(exactly = 1) { restartedConsumer.commitSync() }
    }

    @Test
    fun `close delegates consumer lifecycle without closing projection`() {
        val consumer = mockk<KafkaConsumer<String, String>>()
        val projection = mockk<RedisOrderSummaryProjection>()
        every { consumer.close() } just Runs

        OrderProjectionEventConsumer(consumer, projection).use { }

        verify(exactly = 1) { consumer.close() }
    }

    private fun record(event: OrderPlaced): ConsumerRecord<String, String> =
        ConsumerRecord("orders", 0, 7L, event.aggregateId.value, OrderDomainEventJsonCodec().encode(event))

    private fun records(record: ConsumerRecord<String, String>): Map<TopicPartition, List<ConsumerRecord<String, String>>> =
        mapOf(TopicPartition(record.topic(), record.partition()) to listOf(record))

    private fun testEvent(): OrderPlaced = OrderPlaced(
        aggregateId = OrderId("order-projection-test"),
        occurredOn = Instant.parse("2026-05-27T00:00:00Z"),
        customerId = CustomerId("customer-projection-test"),
        totalAmount = BigDecimal("12.50").toPlainString(),
    )
}
