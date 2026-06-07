package io.bluetape4k.javers.persistence.kafka.repository

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.javers.repository.event.CdoSnapshotEvent
import io.bluetape4k.javers.repository.event.CdoSnapshotEventCodecIds
import io.bluetape4k.javers.repository.event.CdoSnapshotEventMetadata
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.javers.core.JaversBuilder
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.SnapshotType
import org.javers.core.model.SnapshotEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class VanillaKafkaCdoSnapshotRepositoryTest {

    private val producer = mockk<Producer<String, String>>(relaxed = true)
    private val recordMetadata = mockk<RecordMetadata>()

    @BeforeEach
    fun beforeEach() {
        clearMocks(producer, recordMetadata)
    }

    @Test
    fun `repository publishes encoded snapshot with vanilla producer`() {
        val recordSlot = slot<ProducerRecord<String, String>>()
        every { producer.send(capture(recordSlot)) } returns completedMetadataFuture()

        val repository = VanillaKafkaCdoSnapshotRepository(
            producer = producer,
            options = VanillaKafkaCdoSnapshotRepositoryOptions(topic = "audit.snapshots"),
        )
        val javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()

        repository.codec() shouldBeEqualTo JaversCodecs.String

        javers.commit("vanilla", SnapshotEntity(1).apply { intProperty = 7 })

        val record = recordSlot.captured
        record.topic() shouldBeEqualTo "audit.snapshots"
        record.key() shouldBeEqualTo "org.javers.core.model.SnapshotEntity/1"
        JaversCodecs.String.decode(record.value()).shouldNotBeNull()
        verify(exactly = 0) { producer.flush() }
    }

    @Test
    fun `repository uses custom key mapper`() {
        val recordSlot = slot<ProducerRecord<String, String>>()
        every { producer.send(capture(recordSlot)) } returns completedMetadataFuture()

        val repository = VanillaKafkaCdoSnapshotRepository(
            producer = producer,
            options = VanillaKafkaCdoSnapshotRepositoryOptions(topic = "audit.snapshots"),
            keyMapper = { "snapshot-${it.version}" },
        )
        val javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()

        javers.commit("vanilla", SnapshotEntity(1))

        recordSlot.captured.key() shouldBeEqualTo "snapshot-1"
    }

    @Test
    fun `repository-created producer publishes and closes through bluetape4k kafka helper`() {
        val repository = VanillaKafkaCdoSnapshotRepository(
            producerConfigs = KafkaProvider.producerProperties,
            options = VanillaKafkaCdoSnapshotRepositoryOptions(topic = KafkaProvider.TEST_TOPIC),
        )
        val javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()

        try {
            javers.commit("vanilla", SnapshotEntity(1))
        } finally {
            repository.close()
        }
    }

    @Test
    fun `saveSnapshot propagates RuntimeException when Kafka publish fails`() {
        every { producer.send(any()) } returns CompletableFuture.failedFuture(RuntimeException("Kafka broker unavailable"))

        val repository = VanillaKafkaCdoSnapshotRepository(
            producer = producer,
            options = VanillaKafkaCdoSnapshotRepositoryOptions(topic = "audit.snapshots"),
        )
        val javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()

        assertFailsWith<RuntimeException> {
            javers.commit("vanilla", SnapshotEntity(1))
        }
    }

    @Test
    fun `publisher rejects blank explicit key`() {
        val publisher = VanillaKafkaSnapshotEventPublisher(
            producer = producer,
            options = VanillaKafkaCdoSnapshotRepositoryOptions(topic = "audit.snapshots"),
        )

        assertFailsWith<IllegalArgumentException> {
            publisher.publish(snapshotEvent(), " ")
        }
    }

    @Test
    fun `saveSnapshot propagates timeout when Kafka publish does not complete`() {
        every { producer.send(any()) } returns CompletableFuture<RecordMetadata>()

        val repository = VanillaKafkaCdoSnapshotRepository(
            producer = producer,
            options = VanillaKafkaCdoSnapshotRepositoryOptions(
                topic = "audit.snapshots",
                publishTimeout = Duration.ofMillis(1),
            ),
        )
        val javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()

        assertFailsWith<RuntimeException> {
            javers.commit("vanilla", SnapshotEntity(1))
        }
    }

    @Test
    fun `saveSnapshot restores interrupt status when Kafka publish is interrupted`() {
        every { producer.send(any()) } returns InterruptedFuture()

        val repository = VanillaKafkaCdoSnapshotRepository(
            producer = producer,
            options = VanillaKafkaCdoSnapshotRepositoryOptions(topic = "audit.snapshots"),
        )
        val javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()

        try {
            assertFailsWith<RuntimeException> {
                javers.commit("vanilla", SnapshotEntity(1))
            }
            Thread.currentThread().isInterrupted.shouldBeTrue()
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun `flushAfterSend flushes producer only after successful acknowledgement`() {
        every { producer.send(any()) } returns completedMetadataFuture()
        every { producer.flush() } just Runs

        val repository = VanillaKafkaCdoSnapshotRepository(
            producer = producer,
            options = VanillaKafkaCdoSnapshotRepositoryOptions(
                topic = "audit.snapshots",
                flushAfterSend = true,
            ),
        )
        val javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()

        javers.commit("vanilla", SnapshotEntity(1))

        verify(exactly = 1) { producer.flush() }
    }

    @Test
    fun `close does not close caller owned producer by default`() {
        val repository = VanillaKafkaCdoSnapshotRepository(
            producer = producer,
            options = VanillaKafkaCdoSnapshotRepositoryOptions(topic = "audit.snapshots"),
        )

        repository.close()

        verify(exactly = 0) { producer.close(any<Duration>()) }
    }

    @Test
    fun `close closes producer when ownership is enabled`() {
        every { producer.close(any<Duration>()) } just Runs
        val repository = VanillaKafkaCdoSnapshotRepository(
            producer = producer,
            options = VanillaKafkaCdoSnapshotRepositoryOptions(
                topic = "audit.snapshots",
                publishTimeout = Duration.ofSeconds(5),
                closeProducerOnClose = true,
            ),
        )

        repository.close()

        verify(exactly = 1) { producer.close(Duration.ofSeconds(5)) }
    }

    @Test
    fun `options reject blank topic`() {
        assertFailsWith<IllegalArgumentException> {
            VanillaKafkaCdoSnapshotRepositoryOptions(topic = " ")
        }
    }

    @Test
    fun `options reject non-positive timeout`() {
        assertFailsWith<IllegalArgumentException> {
            VanillaKafkaCdoSnapshotRepositoryOptions(
                topic = "audit.snapshots",
                publishTimeout = Duration.ZERO,
            )
        }
    }

    @Test
    fun `rebuilt repository has no head because vanilla Kafka persistence is write only`() {
        every { producer.send(any()) } returns completedMetadataFuture()
        val options = VanillaKafkaCdoSnapshotRepositoryOptions(topic = "audit.snapshots")
        val repository = VanillaKafkaCdoSnapshotRepository(producer, options)
        val javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()
        val commit = javers.commit("vanilla", SnapshotEntity(1))

        repository.getHeadId() shouldBeEqualTo commit.id

        val rebuiltRepository = VanillaKafkaCdoSnapshotRepository(producer, options)
        JaversBuilder.javers()
            .registerJaversRepository(rebuiltRepository)
            .build()

        rebuiltRepository.getHeadId().shouldBeNull()
    }

    @Test
    fun `read path keeps write only contract and warns once per repository`() {
        val repository = VanillaKafkaCdoSnapshotRepository(
            producer = producer,
            options = VanillaKafkaCdoSnapshotRepositoryOptions(topic = "audit.snapshots"),
        )
        val (logger, appender) = attachLogAppender()

        try {
            repeat(2) {
                repository.invokeGetKeys().shouldBeEmpty()
                repository.invokeContains("global-id").shouldBeFalse()
                repository.invokeGetSeq(CommitId.valueOf("1.0")) shouldBeEqualTo 0L
                repository.invokeGetSnapshotSize("global-id") shouldBeEqualTo 0
                repository.loadSnapshots("global-id").shouldBeEmpty()
            }
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }

        val contractLogs = appender.list.filter {
            it.formattedMessage.contains("VanillaKafkaCdoSnapshotRepository is write-only;")
        }

        contractLogs.count { it.level == Level.WARN } shouldBeEqualTo 1
        contractLogs.count { it.level == Level.DEBUG } shouldBeEqualTo 9
    }

    private fun completedMetadataFuture(): CompletableFuture<RecordMetadata> =
        CompletableFuture.completedFuture(recordMetadata)

    private fun snapshotEvent(): CdoSnapshotEvent<String> =
        CdoSnapshotEvent(
            metadata = CdoSnapshotEventMetadata(
                globalIdValue = "Entity/1",
                commitId = "1.00",
                commitMajorId = 1L,
                commitMinorId = 0,
                repositorySequence = null,
                snapshotVersion = 1L,
                snapshotType = SnapshotType.INITIAL.name,
                author = "author",
                commitTimestamp = Instant.EPOCH,
                codecId = CdoSnapshotEventCodecIds.JSON_STRING,
                idempotencyKey = "Entity/1:1.00:1",
            ),
            payload = "{}",
        )

    private fun AbstractCdoSnapshotRepository<*>.codec(): Any {
        val field = AbstractCdoSnapshotRepository::class.java.getDeclaredField("codec")
        field.isAccessible = true
        return field.get(this)
    }

    private fun attachLogAppender(): Pair<Logger, ListAppender<ILoggingEvent>> {
        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        return logger to appender
    }

    @Suppress("UNCHECKED_CAST")
    private fun VanillaKafkaCdoSnapshotRepository.invokeGetKeys(): Set<String> {
        val method = VanillaKafkaCdoSnapshotRepository::class.java.getDeclaredMethod("getKeys")
        method.isAccessible = true
        return method.invoke(this) as Set<String>
    }

    private fun VanillaKafkaCdoSnapshotRepository.invokeContains(globalIdValue: String): Boolean {
        val method = VanillaKafkaCdoSnapshotRepository::class.java.getDeclaredMethod("contains", String::class.java)
        method.isAccessible = true
        return method.invoke(this, globalIdValue) as Boolean
    }

    private fun VanillaKafkaCdoSnapshotRepository.invokeGetSeq(commitId: CommitId): Long {
        val method = VanillaKafkaCdoSnapshotRepository::class.java.getDeclaredMethod("getSeq", CommitId::class.java)
        method.isAccessible = true
        return method.invoke(this, commitId) as Long
    }

    private fun VanillaKafkaCdoSnapshotRepository.invokeGetSnapshotSize(globalIdValue: String): Int {
        val method = VanillaKafkaCdoSnapshotRepository::class.java.getDeclaredMethod("getSnapshotSize", String::class.java)
        method.isAccessible = true
        return method.invoke(this, globalIdValue) as Int
    }

    private class InterruptedFuture: Future<RecordMetadata> {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
        override fun isCancelled(): Boolean = false
        override fun isDone(): Boolean = false
        override fun get(): RecordMetadata = throw InterruptedException("interrupted")
        override fun get(timeout: Long, unit: TimeUnit): RecordMetadata = throw InterruptedException("interrupted")
    }
}
