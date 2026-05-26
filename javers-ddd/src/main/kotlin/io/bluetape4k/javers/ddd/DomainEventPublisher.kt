package io.bluetape4k.javers.ddd

/**
 * Synchronous publisher for [DomainEvent] instances.
 *
 * ## Contract
 * Implementations should fail fast. [AggregateRepository] calls publishers only
 * after aggregate persistence and JaVers commit have succeeded.
 */
fun interface DomainEventPublisher {

    /**
     * Publishes one domain event.
     */
    fun publish(event: DomainEvent)

    /**
     * Publishes events in iteration order.
     */
    fun publishAll(events: Iterable<DomainEvent>) {
        events.forEach(::publish)
    }
}

/**
 * Publisher that intentionally drops all events.
 */
object NoopDomainEventPublisher: DomainEventPublisher {
    override fun publish(event: DomainEvent) = Unit
}

/**
 * Publisher backed by a Kotlin function.
 */
class FunctionDomainEventPublisher(
    private val publishFunction: (DomainEvent) -> Unit,
): DomainEventPublisher {

    override fun publish(event: DomainEvent) {
        publishFunction(event)
    }
}

/**
 * Publisher that forwards each event to all configured [publishers].
 */
class CompositeDomainEventPublisher(
    private val publishers: List<DomainEventPublisher>,
): DomainEventPublisher {

    constructor(vararg publishers: DomainEventPublisher): this(publishers.toList())

    override fun publish(event: DomainEvent) {
        publishers.forEach { it.publish(event) }
    }
}
