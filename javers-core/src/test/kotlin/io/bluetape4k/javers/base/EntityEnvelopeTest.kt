package io.bluetape4k.javers.base

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.javers.examples.Person
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

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

    @Test
    fun `headers stay outside structural identity but survive java serialization`() {
        val envelope = EntityEnvelope(Person("bob", "Bob")).apply { addHeader("traceId", "trace-1") }
        val entity = envelope.entity.shouldNotBeNull()
        val expected = EntityEnvelope(entity)

        val copied = envelope.copy()
        copied.headers shouldBeEqualTo emptyMap()
        copied shouldBeEqualTo expected
        envelope.hashCode() shouldBeEqualTo expected.hashCode()
        envelope.toString() shouldBeEqualTo expected.toString()

        val bytes = ByteArrayOutputStream().also { output ->
            ObjectOutputStream(output).use { it.writeObject(envelope) }
        }.toByteArray()
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as EntityEnvelope }
        restored.headers shouldBeEqualTo mapOf("traceId" to "trace-1")
    }
}
