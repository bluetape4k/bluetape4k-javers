package io.bluetape4k.javers.ddd

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Instant

class DomainEventPublisherTest {

    @Test
    fun `function publisher delegates event`() {
        val published = mutableListOf<DomainEvent>()
        val publisher = FunctionDomainEventPublisher { published += it }
        val event = TestEvent(aggregateId = "order-1")

        publisher.publish(event)

        published.single() shouldBeEqualTo event
    }

    @Test
    fun `composite publisher preserves publisher order`() {
        val calls = mutableListOf<String>()
        val publisher = CompositeDomainEventPublisher(
            FunctionDomainEventPublisher { calls += "first" },
            FunctionDomainEventPublisher { calls += "second" },
        )

        publisher.publish(TestEvent(aggregateId = "order-1"))

        calls shouldBeEqualTo listOf("first", "second")
    }

    @Test
    fun `noop publisher drops event`() {
        NoopDomainEventPublisher.publish(TestEvent(aggregateId = "order-1"))
    }

    data class TestEvent(
        override val aggregateId: String,
        override val occurredOn: Instant = Instant.parse("2026-05-26T00:00:00Z"),
    ): DomainEvent
}
