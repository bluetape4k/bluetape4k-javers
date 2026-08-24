package io.bluetape4k.javers.dispatcher

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotContain
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

        dispatcher.isSaved(person).shouldBeTrue()
        dispatcher.isDeleted(person).shouldBeTrue()
        dispatcher.isDeletedById("alice", Person::class.java).shouldBeTrue()
        recorder.events shouldHaveSize 3

        dispatcher.clear()

        dispatcher.isSaved(person).shouldBeFalse()
        dispatcher.isDeleted(person).shouldBeFalse()
        dispatcher.isDeletedById("alice", Person::class.java).shouldBeFalse()
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
        output shouldContain "Send saved domain object. type=String"
        output shouldContain "Send deleted domain object. type=String"
        output shouldContain "Send deleted domain object by id. type=Person"
    }

    @Test
    fun `console dispatcher does not expose payload or identifier values`() {
        val out = ByteArrayOutputStream()
        val originalOut = System.out
        val dispatcher = ConsoleDispatcher()
        val payload = SensitivePayload(token = "secret-token")
        val sensitiveId = "secret-id-42"

        try {
            System.setOut(PrintStream(out))

            dispatcher.sendSaved(payload)
            dispatcher.sendDeleted(payload)
            dispatcher.sendDeletedById(sensitiveId, SensitivePayload::class.java)
        } finally {
            System.setOut(originalOut)
        }

        val output = out.toString()
        output shouldContain "Send saved domain object. type=SensitivePayload"
        output shouldContain "Send deleted domain object. type=SensitivePayload"
        output shouldContain "Send deleted domain object by id. type=SensitivePayload"
        output shouldNotContain payload.token
        output shouldNotContain sensitiveId
    }

    @Test
    fun `slf4j dispatcher accepts all event kinds`() {
        val dispatcher = Slf4jDispatcher(LoggerFactory.getLogger("javers-dispatcher-test"))

        dispatcher.sendSaved("saved-entity")
        dispatcher.sendDeleted("deleted-entity")
        dispatcher.sendDeletedById("id-1", Person::class.java)
    }

    @Test
    fun `slf4j dispatcher does not expose payload or identifier values`() {
        val logger = LoggerFactory.getLogger("javers-dispatcher-redaction-test") as Logger
        val previousLevel = logger.level
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        val payload = SensitivePayload(token = "secret-token")
        val sensitiveId = "secret-id-42"
        logger.level = Level.INFO
        logger.addAppender(appender)

        try {
            val dispatcher = Slf4jDispatcher(logger)

            dispatcher.sendSaved(payload)
            dispatcher.sendDeleted(payload)
            dispatcher.sendDeletedById(sensitiveId, SensitivePayload::class.java)

            val messages = appender.list.map { it.formattedMessage }
            messages shouldHaveSize 3
            messages[0] shouldContain "Send saved domain object. type=SensitivePayload"
            messages[1] shouldContain "Send deleted domain object. type=SensitivePayload"
            messages[2] shouldContain "Send deleted domain object by id. type=SensitivePayload"
            messages.forEach { message ->
                message shouldNotContain payload.token
                message shouldNotContain sensitiveId
            }
        } finally {
            logger.detachAppender(appender)
            logger.level = previousLevel
            appender.stop()
        }
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

    private data class SensitivePayload(val token: String) {
        override fun toString(): String = "SensitivePayload(token=$token)"
    }
}
