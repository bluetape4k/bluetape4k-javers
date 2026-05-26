package io.bluetape4k.javers.ddd.spring

import io.bluetape4k.javers.ddd.DomainEvent
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant

class SpringApplicationEventDomainEventPublisherTest {

    @Test
    fun `publisher delegates to Spring application event publisher`() {
        val applicationEventPublisher = mockk<ApplicationEventPublisher>()
        val event = TestEvent(aggregateId = "order-1")
        every { applicationEventPublisher.publishEvent(event) } returns Unit

        SpringApplicationEventDomainEventPublisher(applicationEventPublisher).publish(event)

        verify(exactly = 1) { applicationEventPublisher.publishEvent(event) }
        confirmVerified(applicationEventPublisher)
    }

    data class TestEvent(
        override val aggregateId: String,
        override val occurredOn: Instant = Instant.parse("2026-05-26T00:00:00Z"),
    ): DomainEvent
}
