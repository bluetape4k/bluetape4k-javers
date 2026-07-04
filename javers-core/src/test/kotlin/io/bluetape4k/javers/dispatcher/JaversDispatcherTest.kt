package io.bluetape4k.javers.dispatcher

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.javers.dispatcher.internal.CompositeDispatcher
import io.bluetape4k.javers.dispatcher.internal.ConsoleDispatcher
import io.bluetape4k.javers.dispatcher.internal.DebugDispatcher
import io.bluetape4k.javers.dispatcher.internal.Slf4jDispatcher
import io.bluetape4k.javers.examples.Person
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class JaversDispatcherTest {

    @Test
    fun `composite dispatcher forwards all event kinds and ignores failing delegates`() {
        val recorder = RecordingDispatcher()
        val dispatcher = CompositeDispatcher(listOf(FailingDispatcher(), recorder))
        val person = Person("bob", "Bob")

        dispatcher.sendSaved(person)
        dispatcher.sendDeleted(person)
        dispatcher.sendDeletedById<Person>("bob")

        recorder.events shouldBeEqualTo listOf(
            "saved:bob",
            "deleted:bob",
            "deletedById:bob:Person",
        )
    }

    @Test
    fun `debug dispatcher records events and clears state`() {
        val recorder = RecordingDispatcher()
        val dispatcher = DebugDispatcher(listOf(recorder))
        val person = Person("alice", "Alice")

        dispatcher.sendSaved(person)
        dispatcher.sendDeleted(person)
        dispatcher.sendDeletedById<Person>("alice")

        dispatcher.isSaved(person) shouldBeEqualTo true
        dispatcher.isDeleted(person) shouldBeEqualTo true
        dispatcher.isDeletedById("alice", Person::class.java) shouldBeEqualTo true
        recorder.events shouldHaveSize 3

        dispatcher.clear()

        dispatcher.isSaved(person) shouldBeEqualTo false
        dispatcher.isDeleted(person) shouldBeEqualTo false
        dispatcher.isDeletedById("alice", Person::class.java) shouldBeEqualTo false
    }

    @Test
    fun `console dispatcher writes all event messages to standard output`() {
        val out = ByteArrayOutputStream()
        val originalOut = System.out
        val dispatcher = ConsoleDispatcher()

        try {
            System.setOut(PrintStream(out))

            dispatcher.sendSaved("saved-entity")
            dispatcher.sendDeleted("deleted-entity")
            dispatcher.sendDeletedById("id-1", Person::class.java)
        } finally {
            System.setOut(originalOut)
        }

        val output = out.toString()
        output shouldContain "Send saved domain object. saved-entity"
        output shouldContain "Send deleted domain object. deleted-entity"
        output shouldContain "Send deleted domain object by id. id=id-1"
    }

    @Test
    fun `slf4j dispatcher accepts all event kinds`() {
        val dispatcher = Slf4jDispatcher(LoggerFactory.getLogger("javers-dispatcher-test"))

        dispatcher.sendSaved("saved-entity")
        dispatcher.sendDeleted("deleted-entity")
        dispatcher.sendDeletedById("id-1", Person::class.java)
    }

    private class RecordingDispatcher: JaversDispatcher {
        val events = mutableListOf<String>()

        override fun sendSaved(domainObject: Any) {
            events += "saved:${(domainObject as Person).login}"
        }

        override fun sendDeleted(domainObject: Any) {
            events += "deleted:${(domainObject as Person).login}"
        }

        override fun sendDeletedById(domainObjectId: Any, domainType: Class<*>) {
            events += "deletedById:$domainObjectId:${domainType.simpleName}"
        }
    }

    private class FailingDispatcher: JaversDispatcher {
        override fun sendSaved(domainObject: Any) {
            error("save failure")
        }

        override fun sendDeleted(domainObject: Any) {
            error("delete failure")
        }

        override fun sendDeletedById(domainObjectId: Any, domainType: Class<*>) {
            error("delete-by-id failure")
        }
    }
}
