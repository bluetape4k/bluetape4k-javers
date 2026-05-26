package io.bluetape4k.javers.ddd.nats

import io.bluetape4k.javers.ddd.DomainEvent
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.nats.client.Connection
import org.junit.jupiter.api.Test
import java.time.Instant

class NatsDomainEventPublisherTest {

    @Test
    fun `publisher sends event to resolved subject`() {
        val connection = mockk<Connection>()
        val event = TestEvent(aggregateId = "order-1")
        val payload = "order-1".toByteArray()
        every { connection.publish("orders.placed", payload) } just Runs

        NatsDomainEventPublisher(
            connection = connection,
            subjectResolver = { "orders.placed" },
            serializer = { it.aggregateId.toString().toByteArray() },
        ).publish(event)

        verify(exactly = 1) { connection.publish("orders.placed", payload) }
        confirmVerified(connection)
    }

    data class TestEvent(
        override val aggregateId: String,
        override val occurredOn: Instant = Instant.parse("2026-05-26T00:00:00Z"),
    ): DomainEvent
}
