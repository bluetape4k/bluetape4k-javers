package io.bluetape4k.javers.ddd

import java.time.Instant

/**
 * Domain event emitted by an [AggregateRoot].
 *
 * ## Contract
 * Events are intentionally modeled as an interface, not a sealed class, so
 * consumers can declare event types in their own modules. [attributes] are
 * copied into JaVers commit properties by [toJaversProperties].
 *
 * ```kotlin
 * data class OrderPlaced(
 *     override val aggregateId: Long,
 *     override val occurredOn: Instant = Instant.now(),
 * ) : DomainEvent
 * ```
 */
interface DomainEvent {

    /**
     * Identifier of the aggregate that emitted this event.
     */
    val aggregateId: Any

    /**
     * Time when the domain event happened.
     */
    val occurredOn: Instant

    /**
     * Optional event-specific metadata stored as JaVers commit properties.
     */
    val attributes: Map<String, String>
        get() = emptyMap()
}

/**
 * Converts a [DomainEvent] to stable JaVers commit properties.
 *
 * ## Contract
 * Reserved properties describe the event type, aggregate id, and occurrence
 * time. User attributes are namespaced under `event.` to avoid collisions.
 */
fun DomainEvent.toJaversProperties(): Map<String, String> {
    val properties = linkedMapOf(
        DOMAIN_EVENT_TYPE_PROPERTY to eventTypeName(),
        DOMAIN_EVENT_AGGREGATE_ID_PROPERTY to aggregateId.toString(),
        DOMAIN_EVENT_OCCURRED_ON_PROPERTY to occurredOn.toString(),
    )
    attributes.forEach { (key, value) ->
        properties["$DOMAIN_EVENT_ATTRIBUTE_PREFIX$key"] = value
    }
    return properties
}

internal fun Collection<DomainEvent>.toJaversProperties(): Map<String, String> = when (size) {
    0 -> emptyMap()
    1 -> first().toJaversProperties()
    else -> linkedMapOf(
        DOMAIN_EVENT_COUNT_PROPERTY to size.toString(),
        DOMAIN_EVENT_TYPES_PROPERTY to joinToString(",") { it.eventTypeName() },
    )
}

internal fun DomainEvent.eventTypeName(): String {
    return this::class.qualifiedName ?: this::class.java.name
}

const val DOMAIN_EVENT_TYPE_PROPERTY: String = "domainEventType"
const val DOMAIN_EVENT_TYPES_PROPERTY: String = "domainEventTypes"
const val DOMAIN_EVENT_COUNT_PROPERTY: String = "domainEventCount"
const val DOMAIN_EVENT_AGGREGATE_ID_PROPERTY: String = "aggregateId"
const val DOMAIN_EVENT_OCCURRED_ON_PROPERTY: String = "occurredOn"
const val DOMAIN_EVENT_ATTRIBUTE_PREFIX: String = "event."
