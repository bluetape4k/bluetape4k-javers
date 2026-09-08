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

    /** 외부 adapter가 원래 이벤트 타입을 보존할 때 재정의합니다. */
    val eventType: String
        get() = this::class.qualifiedName ?: this::class.java.name

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

/**
 * 0개는 빈 metadata, 1개는 기존 키, 여러 개는 기존 요약과 `events.<index>.`별 metadata를 반환합니다.
 * index는 입력 컬렉션의 순회 순서이며 사용자 속성은 각 이벤트의 `event.` 아래에 보존합니다.
 * 크기 제한이나 자동 절단은 적용하지 않습니다. 호출자는 backend의 commit property 제한에 맞게
 * 이벤트 수와 속성을 제한해야 하며, 큰 이벤트 본문은 별도의 event store에 저장합니다.
 */
internal fun Collection<DomainEvent>.toJaversProperties(): Map<String, String> = when (size) {
    0 -> emptyMap()
    1 -> first().toJaversProperties()
    else -> linkedMapOf(
        DOMAIN_EVENT_COUNT_PROPERTY to size.toString(),
        DOMAIN_EVENT_TYPES_PROPERTY to joinToString(",") { it.eventTypeName() },
    ).apply {
        this@toJaversProperties.forEachIndexed { index, event ->
            event.toJaversProperties().forEach { (key, value) ->
                put("events.$index.$key", value)
            }
        }
    }
}

internal fun DomainEvent.eventTypeName(): String {
    return eventType
}

const val DOMAIN_EVENT_TYPE_PROPERTY: String = "domainEventType"
const val DOMAIN_EVENT_TYPES_PROPERTY: String = "domainEventTypes"
const val DOMAIN_EVENT_COUNT_PROPERTY: String = "domainEventCount"
const val DOMAIN_EVENT_AGGREGATE_ID_PROPERTY: String = "aggregateId"
const val DOMAIN_EVENT_OCCURRED_ON_PROPERTY: String = "occurredOn"
const val DOMAIN_EVENT_ATTRIBUTE_PREFIX: String = "event."
