package io.bluetape4k.javers.examples.exposedddd.messaging

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.javers.examples.exposedddd.domain.CustomerId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderPlaced
import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class OrderKafkaEventPublisherTest {

    @Test
    fun `publisher restores interrupt status and hides event details when send is interrupted`() {
        val producer = mockk<Producer<String, String>>()
        val event = testEvent()
        val payload = OrderDomainEventJsonCodec().encode(event)
        every { producer.send(any()) } returns InterruptedFuture()

        try {
            val thrown = assertFailsWith<RuntimeException> {
                publisher(producer).publish(event)
            }

            thrown.cause shouldBeInstanceOf InterruptedException::class
            Thread.currentThread().isInterrupted.shouldBeTrue()
            val message = thrown.message.orEmpty()
            message shouldContain "topic=orders"
            message.shouldNotContain(event.aggregateId.value)
            message.shouldNotContain(payload)
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun `publisher distinguishes timeout without exposing event payload`() {
        val producer = mockk<Producer<String, String>>()
        val event = testEvent()
        val payload = OrderDomainEventJsonCodec().encode(event)
        every { producer.send(any()) } returns TimeoutFuture()

        val thrown = assertFailsWith<RuntimeException> {
            publisher(producer).publish(event)
        }

        thrown.cause shouldBeInstanceOf TimeoutException::class
        val message = thrown.message.orEmpty()
        message shouldContain "timed out"
        message shouldContain "topic=orders"
        message.shouldNotContain(event.aggregateId.value)
        message.shouldNotContain(payload)
    }

    @Test
    fun `publisher unwraps producer failure and keeps diagnostics safe`() {
        val producer = mockk<Producer<String, String>>()
        val event = testEvent()
        val payload = OrderDomainEventJsonCodec().encode(event)
        val failure = IllegalStateException("broker unavailable")
        every { producer.send(any()) } returns FailedFuture(failure)

        val thrown = assertFailsWith<RuntimeException> {
            publisher(producer).publish(event)
        }

        thrown.cause shouldBeSameInstanceAs failure
        val message = thrown.message.orEmpty()
        message shouldContain "failed"
        message shouldContain "topic=orders"
        message.shouldNotContain(event.aggregateId.value)
        message.shouldNotContain(payload)
    }

    private fun publisher(producer: Producer<String, String>): OrderKafkaEventPublisher =
        OrderKafkaEventPublisher(producer = producer, topic = "orders")

    private fun testEvent(): OrderPlaced =
        OrderPlaced(
            aggregateId = OrderId("order-sensitive"),
            occurredOn = Instant.parse("2026-05-27T00:00:00Z"),
            customerId = CustomerId("customer-sensitive"),
            totalAmount = "99.99",
        )

    private class InterruptedFuture: Future<RecordMetadata> {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
        override fun isCancelled(): Boolean = false
        override fun isDone(): Boolean = false
        override fun get(): RecordMetadata = throw InterruptedException("interrupted")
        override fun get(timeout: Long, unit: TimeUnit): RecordMetadata = throw InterruptedException("interrupted")
    }

    private class TimeoutFuture: Future<RecordMetadata> {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
        override fun isCancelled(): Boolean = false
        override fun isDone(): Boolean = false
        override fun get(): RecordMetadata = throw TimeoutException("timed out")
        override fun get(timeout: Long, unit: TimeUnit): RecordMetadata = throw TimeoutException("timed out")
    }

    private class FailedFuture(
        private val failure: Throwable,
    ): Future<RecordMetadata> {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
        override fun isCancelled(): Boolean = false
        override fun isDone(): Boolean = true
        override fun get(): RecordMetadata = throw ExecutionException(failure)
        override fun get(timeout: Long, unit: TimeUnit): RecordMetadata = throw ExecutionException(failure)
    }
}
