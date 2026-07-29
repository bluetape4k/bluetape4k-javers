package io.bluetape4k.javers.ddd

/**
 * [DomainEvent] instance를 위한 synchronous publisher입니다.
 *
 * ## 계약
 * 구현체는 fail fast해야 합니다. [AggregateRepository]는 aggregate persistence와 JaVers commit이
 * 성공한 뒤에만 publisher를 호출합니다.
 */
fun interface DomainEventPublisher {

    /**
     * domain event 하나를 publish합니다.
     */
    fun publish(event: DomainEvent)

    /**
     * event를 iteration 순서대로 publish합니다.
     */
    fun publishAll(events: Iterable<DomainEvent>) {
        events.forEach(::publish)
    }
}

/**
 * 모든 event를 의도적으로 버리는 publisher입니다.
 */
object NoopDomainEventPublisher: DomainEventPublisher {
    override fun publish(event: DomainEvent) = Unit
}

/**
 * Kotlin function으로 구현한 publisher입니다.
 */
class FunctionDomainEventPublisher(
    private val publishFunction: (DomainEvent) -> Unit,
): DomainEventPublisher {

    override fun publish(event: DomainEvent) {
        publishFunction(event)
    }
}

/**
 * 각 event를 구성된 모든 [publishers]로 전달하는 publisher입니다.
 */
class CompositeDomainEventPublisher(
    private val publishers: List<DomainEventPublisher>,
): DomainEventPublisher {

    constructor(vararg publishers: DomainEventPublisher): this(publishers.toList())

    override fun publish(event: DomainEvent) {
        publishers.forEach { it.publish(event) }
    }
}
