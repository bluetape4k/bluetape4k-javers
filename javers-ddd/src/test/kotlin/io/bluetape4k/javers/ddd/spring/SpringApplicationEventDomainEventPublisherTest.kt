package io.bluetape4k.javers.ddd.spring

import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.javers.ddd.DomainEvent
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.io.Serializable
import java.time.Instant

class SpringApplicationEventDomainEventPublisherTest {

    @Test
    fun `publisher delegates immediately when transaction synchronization is inactive`() {
        val applicationEventPublisher = RecordingApplicationEventPublisher()
        val event = TestEvent(aggregateId = "order-1")

        SpringApplicationEventDomainEventPublisher(applicationEventPublisher).publish(event)

        applicationEventPublisher.events.single() shouldBeSameInstanceAs event
    }

    @Test
    fun `publisher publishes event after transaction commit`() {
        val applicationEventPublisher = RecordingApplicationEventPublisher()
        val publisher = SpringApplicationEventDomainEventPublisher(applicationEventPublisher)
        val event = TestEvent(aggregateId = "order-2")
        val transactionTemplate = TransactionTemplate(LocalTransactionManager())

        transactionTemplate.executeWithoutResult {
            publisher.publish(event)

            applicationEventPublisher.events.shouldBeEmpty()
        }

        applicationEventPublisher.events.single() shouldBeSameInstanceAs event
    }

    @Test
    fun `publisher does not publish event after transaction rollback`() {
        val applicationEventPublisher = RecordingApplicationEventPublisher()
        val publisher = SpringApplicationEventDomainEventPublisher(applicationEventPublisher)
        val event = TestEvent(aggregateId = "order-3")
        val transactionTemplate = TransactionTemplate(LocalTransactionManager())

        transactionTemplate.executeWithoutResult { status ->
            publisher.publish(event)
            status.setRollbackOnly()

            applicationEventPublisher.events.shouldBeEmpty()
        }

        applicationEventPublisher.events.shouldBeEmpty()
    }

    private class RecordingApplicationEventPublisher: ApplicationEventPublisher {
        val events = mutableListOf<Any>()

        override fun publishEvent(event: ApplicationEvent) {
            events += event
        }

        override fun publishEvent(event: Any) {
            events += event
        }
    }

    private class LocalTransactionManager: AbstractPlatformTransactionManager() {
        override fun doGetTransaction(): Any = Any()

        override fun doBegin(transaction: Any, definition: TransactionDefinition) = Unit

        override fun doCommit(status: DefaultTransactionStatus) = Unit

        override fun doRollback(status: DefaultTransactionStatus) = Unit
    }

    private data class TestEvent(
        override val aggregateId: String,
        override val occurredOn: Instant = Instant.parse("2026-05-26T00:00:00Z"),
    ): DomainEvent, Serializable {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}
