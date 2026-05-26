package io.bluetape4k.javers.ddd

import io.bluetape4k.assertions.shouldBeEqualTo
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

    data class OrderPlaced(
        override val aggregateId: Long,
        override val occurredOn: Instant,
        override val attributes: Map<String, String> = emptyMap(),
    ): DomainEvent
}
