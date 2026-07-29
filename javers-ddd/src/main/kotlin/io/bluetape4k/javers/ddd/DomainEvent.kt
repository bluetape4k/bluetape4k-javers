package io.bluetape4k.javers.ddd

import java.time.Instant

/**
 * [AggregateRoot]가 emit한 domain event입니다.
 *
 * ## 계약
 * consumer가 자신의 module에서 event type을 선언할 수 있도록 event는 sealed class가 아니라
 * interface로 의도적으로 modeling합니다. [attributes]는 [toJaversProperties]에 의해
 * JaVers commit properties로 copy됩니다.
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
     * 이 event를 emit한 aggregate의 identifier입니다.
     */
    val aggregateId: Any

    /**
     * domain event가 발생한 시간입니다.
     */
    val occurredOn: Instant

    /**
     * JaVers commit properties로 저장할 optional event-specific metadata입니다.
     */
    val attributes: Map<String, String>
        get() = emptyMap()
}

/**
 * [DomainEvent]를 stable JaVers commit properties로 변환합니다.
 *
 * ## 계약
 * reserved property는 event type, aggregate id, occurrence time을 설명합니다.
 * 사용자 attribute는 collision을 피하기 위해 `event.` namespace 아래에 배치합니다.
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
