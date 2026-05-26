package io.bluetape4k.javers.ddd.kafka

import io.bluetape4k.javers.ddd.DomainEvent
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.Instant
import java.util.concurrent.CompletableFuture

class KafkaDomainEventPublisherTest {

    @Test
    fun `publisher sends event to resolved topic and key`() {
        val kafkaTemplate = mockk<KafkaTemplate<String, DomainEvent>>()
        val sendResult = mockk<SendResult<String, DomainEvent>>()
        val event = TestEvent(aggregateId = "order-1")
        every {
            kafkaTemplate.send("domain-events", "order-1", event)
        } returns CompletableFuture.completedFuture(sendResult)

        KafkaDomainEventPublisher(
            kafkaTemplate = kafkaTemplate,
            topicResolver = { "domain-events" },
        ).publish(event)

        verify(exactly = 1) { kafkaTemplate.send("domain-events", "order-1", event) }
        confirmVerified(kafkaTemplate)
    }

    data class TestEvent(
        override val aggregateId: String,
        override val occurredOn: Instant = Instant.parse("2026-05-26T00:00:00Z"),
    ): DomainEvent
}
