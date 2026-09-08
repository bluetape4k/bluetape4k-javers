package io.bluetape4k.javers.ddd

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import org.junit.jupiter.api.Test
import java.time.Instant

class DomainEventTest {

    @Test
    fun `domain event maps to JaVers commit properties`() {
        val occurredOn = Instant.parse("2026-05-26T00:00:00Z")
        val event = OrderPlaced(
            aggregateId = 42L,
            occurredOn = occurredOn,
            attributes = mapOf("tenant" to "blue", "source" to "test"),
        )

        val properties = event.toJaversProperties()

        properties[DOMAIN_EVENT_TYPE_PROPERTY] shouldBeEqualTo OrderPlaced::class.qualifiedName
        properties[DOMAIN_EVENT_AGGREGATE_ID_PROPERTY] shouldBeEqualTo "42"
        properties[DOMAIN_EVENT_OCCURRED_ON_PROPERTY] shouldBeEqualTo occurredOn.toString()
        properties["event.tenant"] shouldBeEqualTo "blue"
        properties["event.source"] shouldBeEqualTo "test"
    }

    @Test
    fun `domain event uses empty attributes by default`() {
        val occurredOn = Instant.parse("2026-05-26T00:00:00Z")
        val event = OrderPlaced(aggregateId = 43L, occurredOn = occurredOn)

        val properties = event.toJaversProperties()

        properties[DOMAIN_EVENT_TYPE_PROPERTY] shouldBeEqualTo OrderPlaced::class.qualifiedName
        properties[DOMAIN_EVENT_AGGREGATE_ID_PROPERTY] shouldBeEqualTo "43"
        properties[DOMAIN_EVENT_OCCURRED_ON_PROPERTY] shouldBeEqualTo occurredOn.toString()
        properties shouldHaveSize 3
    }

    @Test
    fun `event collection maps empty single and multiple events`() {
        val occurredOn = Instant.parse("2026-05-26T00:00:00Z")
        val placed = OrderPlaced(aggregateId = 44L, occurredOn = occurredOn)
        val shipped = OrderShipped(aggregateId = 44L, occurredOn = occurredOn)

        emptyList<DomainEvent>().toJaversProperties() shouldBeEqualTo emptyMap()
        listOf(placed).toJaversProperties() shouldBeEqualTo placed.toJaversProperties()

        val properties = listOf(placed, shipped).toJaversProperties()

        properties[DOMAIN_EVENT_COUNT_PROPERTY] shouldBeEqualTo "2"
        properties[DOMAIN_EVENT_TYPES_PROPERTY] shouldBeEqualTo
            "${OrderPlaced::class.qualifiedName},${OrderShipped::class.qualifiedName}"
    }

    @Test
    fun `다중 이벤트는 순서별 ID 시각과 충돌하는 사용자 속성을 보존한다`() {
        val time = Instant.parse("2026-09-08T00:00:00Z")
        val first = OrderPlaced(1L, time, mapOf("tenant" to "blue", "aggregateId" to "custom"))
        val second = OrderPlaced(2L, time.plusSeconds(1), mapOf("tenant" to "green"))

        val properties = listOf(first, second).toJaversProperties()

        properties["events.0.aggregateId"] shouldBeEqualTo "1"
        properties["events.0.occurredOn"] shouldBeEqualTo time.toString()
        properties["events.0.event.tenant"] shouldBeEqualTo "blue"
        properties["events.0.event.aggregateId"] shouldBeEqualTo "custom"
        properties["events.1.aggregateId"] shouldBeEqualTo "2"
        properties["events.1.occurredOn"] shouldBeEqualTo time.plusSeconds(1).toString()
        properties["events.1.event.tenant"] shouldBeEqualTo "green"
        properties[DOMAIN_EVENT_COUNT_PROPERTY] shouldBeEqualTo "2"
    }

    data class OrderPlaced(
        override val aggregateId: Long,
        override val occurredOn: Instant,
        override val attributes: Map<String, String> = emptyMap(),
    ): DomainEvent

    data class OrderShipped(
        override val aggregateId: Long,
        override val occurredOn: Instant,
    ): DomainEvent
}
