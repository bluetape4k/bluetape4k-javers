package io.bluetape4k.javers.ddd.spring

import io.bluetape4k.javers.ddd.DomainEvent
import io.bluetape4k.javers.ddd.DomainEventPublisher
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Spring [ApplicationEventPublisher]를 통해 domain event를 publish합니다.
 *
 * ## 계약
 * Spring transaction synchronization이 active이면 event를 `afterCommit`에서 publish합니다.
 * 그렇지 않으면 즉시 publish합니다.
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
