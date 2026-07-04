package io.bluetape4k.javers.base

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.javers.examples.Person
import org.junit.jupiter.api.Test

class EntityEnvelopeTest {

    @Test
    fun `entity constructor creates saved envelope with entity metadata`() {
        val person = Person("bob", "Bob")
        val envelope = EntityEnvelope(person)

        envelope.entity shouldBeEqualTo person
        envelope.entityId.shouldBeNull()
        envelope.entityType shouldBeEqualTo Person::class.java
        envelope.eventType shouldBeEqualTo EntityEventType.SAVED
        envelope.isSavedEntity shouldBeEqualTo true
        envelope.isDeletedEntity shouldBeEqualTo false
    }

    @Test
    fun `id constructor creates deleted envelope and headers are mutable metadata`() {
        val envelope = EntityEnvelope("bob", Person::class.java)

        envelope.entity.shouldBeNull()
        envelope.entityId shouldBeEqualTo "bob"
        envelope.entityType shouldBeEqualTo Person::class.java
        envelope.eventType shouldBeEqualTo EntityEventType.DELETED
        envelope.isSavedEntity shouldBeEqualTo false
        envelope.isDeletedEntity shouldBeEqualTo true

        envelope.addHeader("traceId", "trace-1")
        envelope.getHeader("traceId") shouldBeEqualTo "trace-1"
        envelope.headers["traceId"] shouldBeEqualTo "trace-1"
        envelope.getHeader("missing").shouldBeNull()
    }

    @Test
    fun `entity event type exposes status and nullable lookup`() {
        EntityEventType.UNKNOWN.toString() shouldBeEqualTo "UNKNOWN"
        EntityEventType.SAVED.toString() shouldBeEqualTo "SAVED"
        EntityEventType.DELETED.toString() shouldBeEqualTo "DELETED"

        EntityEventType.Companion.valueOf("UNKNOWN") shouldBeEqualTo EntityEventType.UNKNOWN
        EntityEventType.Companion.valueOf("SAVED") shouldBeEqualTo EntityEventType.SAVED
        EntityEventType.Companion.valueOf("DELETED") shouldBeEqualTo EntityEventType.DELETED
        EntityEventType.Companion.valueOf("unknown").shouldBeNull()
    }
}
