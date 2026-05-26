package io.bluetape4k.javers.ddd.spring

import io.bluetape4k.javers.ddd.DomainEvent
import io.bluetape4k.javers.ddd.DomainEventPublisher
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Publishes domain events through Spring's [ApplicationEventPublisher].
 *
 * ## Contract
 * If Spring transaction synchronization is active, events are published in
 * `afterCommit`. Otherwise, they are published immediately.
 */
class SpringApplicationEventDomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
): DomainEventPublisher {

    override fun publish(event: DomainEvent) {
        publishAfterCommit {
            applicationEventPublisher.publishEvent(event)
        }
    }
}

internal fun publishAfterCommit(block: () -> Unit) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(
            object: TransactionSynchronization {
                override fun afterCommit() {
                    block()
                }
            },
        )
    } else {
        block()
    }
}
