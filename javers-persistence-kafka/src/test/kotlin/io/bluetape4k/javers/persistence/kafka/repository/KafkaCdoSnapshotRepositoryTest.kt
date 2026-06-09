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
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.javers.repository.event.CdoSnapshotEvent
import io.bluetape4k.javers.repository.event.CdoSnapshotEventCodecIds
import io.bluetape4k.javers.repository.event.CdoSnapshotEventMetadata
import io.bluetape4k.logging.KLogging
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.SnapshotType
import org.javers.core.model.PrimitiveEntity
import org.javers.core.model.SnapshotEntity
import org.javers.core.repository.AbstractJaversCommitTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture

/**
 * NOTE: **Redis나 MongoDB와 같이 테스트는 할 수 없고, snapshot 저장을 수행하는 테스트만 해야 한다**
 * Consumer 를 통한 검증을 수행해야 하는데, 귀찮아서 로그보고 확인했습니다. ㅋㅋ
 */
class KafkaCdoSnapshotRepositoryTest: AbstractJaversCommitTest() {

    companion object: KLogging()

    private val kafkaRepository by lazy {
        KafkaCdoSnapshotRepository(KafkaProvider.kafkaTemplate)
    }

    override fun newJavers(): Javers =
        JaversBuilder.javers()
            .registerJaversRepository(kafkaRepository)
            .build()

    val javers = newJavers()

    @Test
    fun `CommitMetadata에 현재 LocalDateTime과 Instant를 사용한다`() {
        val commit = javers.commit("author", SnapshotEntity(1))
        commit.snapshots.size shouldBeEqualTo 1
        val snapshot = commit.snapshots.first()
        snapshot.type shouldBeEqualTo SnapshotType.INITIAL
    }

    @Test
    fun `다양한 Primitive 수형 변화를 Commit한다`() {
        // GIVEN
        val s = PrimitiveEntity("1")

        // WHEN
        javers.commit("author", s)

        s.intField = 10
        s.longField = 10L
        s.doubleField = 1.1
        s.floatField = 1.1F
        s.charField = 'c'
        s.byteField = 10.toByte()
        s.shortField = 10.toShort()
        s.booleanField = true
        s.IntegerField = 10
        s.LongField = 10
        s.DoubleField = 1.1
        s.FloatField = 1.1F
        s.CharField = 'c'
        s.ByteField = 10.toByte()
        s.ShortField = 10.toShort()
        s.BooleanField = true

        val commit = javers.commit("author", s)

        val snapshot = commit.snapshots.first()

        snapshot.state.getPropertyValue("floatField") shouldBeEqualTo 1.1F
        snapshot.state.getPropertyValue("LongField") shouldBeEqualTo 10L
    }

    // Kafka is write-only: loadSnapshots() always returns empty, so Javers always sees
    // no previous state and creates a full initial snapshot on every commit.
    // These tests from AbstractJaversCommitTest assume read capability and cannot pass here.
    @Disabled("Kafka is write-only — second commit always produces a snapshot because loadSnapshots() returns empty")
    override fun `ShallowReferenceType entity snapshot is not committed`() {}

    @Disabled("Kafka is write-only — second commit always produces a snapshot because loadSnapshots() returns empty")
    override fun `changes in a property annotated with @ShallowReference are not committed as a snapshot`() {}

    @Test
    fun `rebuilt repository has no head because Kafka persistence is write only`() {
        val kafkaTemplate = successfulKafkaTemplate()
        val repository = KafkaCdoSnapshotRepository(kafkaTemplate)
        val javersInstance = newJavers(repository)
        val commit = javersInstance.commit("author", SnapshotEntity(1))

        repository.getHeadId() shouldBeEqualTo commit.id

        val rebuiltRepository = KafkaCdoSnapshotRepository(kafkaTemplate)
        newJavers(rebuiltRepository)

        rebuiltRepository.getHeadId().shouldBeNull()
    }

    @Test
    fun `read path keeps write only contract and warns once per repository`() {
        val repository = KafkaCdoSnapshotRepository(successfulKafkaTemplate())
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
            it.formattedMessage.contains("KafkaCdoSnapshotRepository is write-only;")
        }

        contractLogs.count { it.level == Level.WARN } shouldBeEqualTo 1
        contractLogs.count { it.level == Level.DEBUG } shouldBeEqualTo 9
    }

    @Test
    fun `saveSnapshot logs key diagnostics without raw key`() {
        val repository = KafkaCdoSnapshotRepository(successfulKafkaTemplate())
        val javersInstance = newJavers(repository)
        val rawKey = "org.javers.core.model.SnapshotEntity/1"
        val attached = attachTraceLogAppender(KafkaCdoSnapshotRepository::class.java)

        try {
            javersInstance.commit("author", SnapshotEntity(1))
        } finally {
            attached.restore()
        }

        val messages = attached.appender.list.joinToString("\n") { it.formattedMessage }

        messages shouldContain "Produce snapshot."
        messages shouldContain "keyFingerprint="
        messages shouldContain "keyLength=${rawKey.length}"
        messages.shouldNotContain(rawKey)
    }

    @Test
    fun `saveSnapshot propagates RuntimeException when Kafka publish fails`() {
        // Build a KafkaTemplate whose sendDefault always returns a failed future.
        // KafkaTemplate requires a ProducerFactory; supply the real one but override sendDefault
        // so that no actual broker call is made.
        val failingTemplate = object : KafkaTemplate<String, String>(KafkaProvider.producerFactory) {
            override fun sendDefault(key: String, data: String?): CompletableFuture<SendResult<String, String>> =
                CompletableFuture.failedFuture(RuntimeException("Kafka broker unavailable"))
        }
        failingTemplate.setDefaultTopic(KafkaProvider.TEST_TOPIC)

        val repo = KafkaCdoSnapshotRepository(failingTemplate)
        val javersInstance = JaversBuilder.javers()
            .registerJaversRepository(repo)
            .build()

        assertFailsWith<RuntimeException> {
            javersInstance.commit("author", SnapshotEntity(1))
        }
    }

    @Test
    fun `publisher failure message uses key diagnostics without raw key`() {
        val failingTemplate = object : KafkaTemplate<String, String>(KafkaProvider.producerFactory) {
            override fun sendDefault(key: String, data: String?): CompletableFuture<SendResult<String, String>> =
                CompletableFuture.failedFuture(RuntimeException("Kafka broker unavailable"))
        }
        failingTemplate.setDefaultTopic(KafkaProvider.TEST_TOPIC)
        val publisher = KafkaSnapshotEventPublisher(failingTemplate)
        val sensitiveKey = "account:alice@example.com"

        val failure = assertFailsWith<RuntimeException> {
            publisher.publish(snapshotEvent(), sensitiveKey)
        }
        val message = failure.message.orEmpty()

        message shouldContain "keyFingerprint="
        message shouldContain "keyLength=${sensitiveKey.length}"
        message.shouldNotContain(sensitiveKey)
    }

    @Test
    fun `saveSnapshot restores interrupt status when Spring Kafka publish is interrupted`() {
        val interruptedTemplate = object : KafkaTemplate<String, String>(KafkaProvider.producerFactory) {
            override fun sendDefault(key: String, data: String?): CompletableFuture<SendResult<String, String>> =
                InterruptedCompletableFuture()
        }
        interruptedTemplate.setDefaultTopic(KafkaProvider.TEST_TOPIC)

        val repo = KafkaCdoSnapshotRepository(interruptedTemplate)
        val javersInstance = JaversBuilder.javers()
            .registerJaversRepository(repo)
            .build()

        try {
            assertFailsWith<RuntimeException> {
                javersInstance.commit("author", SnapshotEntity(1))
            }
            Thread.currentThread().isInterrupted.shouldBeTrue()
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun `publisher interruption message uses key diagnostics without raw key`() {
        val interruptedTemplate = object : KafkaTemplate<String, String>(KafkaProvider.producerFactory) {
            override fun sendDefault(key: String, data: String?): CompletableFuture<SendResult<String, String>> =
                InterruptedCompletableFuture()
        }
        interruptedTemplate.setDefaultTopic(KafkaProvider.TEST_TOPIC)
        val publisher = KafkaSnapshotEventPublisher(interruptedTemplate)
        val sensitiveKey = "account:alice@example.com"

        try {
            val failure = assertFailsWith<RuntimeException> {
                publisher.publish(snapshotEvent(), sensitiveKey)
            }
            val message = failure.message.orEmpty()

            message shouldContain "keyFingerprint="
            message shouldContain "keyLength=${sensitiveKey.length}"
            message.shouldNotContain(sensitiveKey)
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun `publisher rejects blank explicit key`() {
        val publisher = KafkaSnapshotEventPublisher(successfulKafkaTemplate())

        assertFailsWith<IllegalArgumentException> {
            publisher.publish(snapshotEvent(), " ")
        }
    }

    @Test
    fun `publisher sends to explicit topic when configured`() {
        val kafkaTemplate = explicitTopicKafkaTemplate()
        val publisher = KafkaSnapshotEventPublisher.withTopic(
            kafkaOperations = kafkaTemplate,
            topic = "audit-topic",
        )

        publisher.publish(snapshotEvent(), "Entity/1")

        kafkaTemplate.recordedTopic shouldBeEqualTo "audit-topic"
        kafkaTemplate.recordedKey shouldBeEqualTo "Entity/1"
        kafkaTemplate.sendDefaultCount shouldBeEqualTo 0
    }

    @Test
    fun `publisher rejects blank explicit topic`() {
        assertFailsWith<IllegalArgumentException> {
            KafkaSnapshotEventPublisher.withTopic(
                kafkaOperations = successfulKafkaTemplate(),
                topic = " ",
            )
        }
    }

    @Test
    fun `repository rejects non-positive publish timeout`() {
        assertFailsWith<IllegalArgumentException> {
            KafkaCdoSnapshotRepository(
                kafkaOperations = successfulKafkaTemplate(),
                publishTimeout = Duration.ZERO,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            KafkaCdoSnapshotRepository(
                kafkaOperations = successfulKafkaTemplate(),
                publishTimeout = Duration.ofMillis(-1),
            )
        }
    }

    @Test
    fun `publisher rejects non-positive publish timeout`() {
        assertFailsWith<IllegalArgumentException> {
            KafkaSnapshotEventPublisher(
                kafkaOperations = successfulKafkaTemplate(),
                publishTimeout = Duration.ZERO,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            KafkaSnapshotEventPublisher(
                kafkaOperations = successfulKafkaTemplate(),
                publishTimeout = Duration.ofMillis(-1),
            )
        }
    }

    private fun newJavers(repository: KafkaCdoSnapshotRepository): Javers =
        JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()

    private fun successfulKafkaTemplate(): KafkaTemplate<String, String> {
        return object : KafkaTemplate<String, String>(KafkaProvider.producerFactory) {
            override fun sendDefault(key: String, data: String?): CompletableFuture<SendResult<String, String>> {
                @Suppress("UNCHECKED_CAST")
                return CompletableFuture.completedFuture(null) as CompletableFuture<SendResult<String, String>>
            }
        }.also {
            it.setDefaultTopic(KafkaProvider.TEST_TOPIC)
        }
    }

    private fun explicitTopicKafkaTemplate(): RecordingKafkaTemplate =
        RecordingKafkaTemplate().also {
            it.setDefaultTopic(KafkaProvider.TEST_TOPIC)
        }

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

    private fun attachLogAppender(): Pair<Logger, ListAppender<ILoggingEvent>> {
        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        return logger to appender
    }

    private fun attachTraceLogAppender(loggerClass: Class<*>): AttachedLogAppender {
        val logger = LoggerFactory.getLogger(loggerClass) as Logger
        val previousLevel = logger.level
        val previousAdditive = logger.isAdditive
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.level = Level.TRACE
        logger.isAdditive = false
        logger.addAppender(appender)
        return AttachedLogAppender(logger, appender, previousLevel, previousAdditive)
    }

    private class AttachedLogAppender(
        val logger: Logger,
        val appender: ListAppender<ILoggingEvent>,
        val previousLevel: Level?,
        val previousAdditive: Boolean,
    ) {
        fun restore() {
            logger.detachAppender(appender)
            logger.level = previousLevel
            logger.isAdditive = previousAdditive
            appender.stop()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun KafkaCdoSnapshotRepository.invokeGetKeys(): Set<String> {
        val method = KafkaCdoSnapshotRepository::class.java.getDeclaredMethod("getKeys")
        method.isAccessible = true
        return method.invoke(this) as Set<String>
    }

    private fun KafkaCdoSnapshotRepository.invokeContains(globalIdValue: String): Boolean {
        val method = KafkaCdoSnapshotRepository::class.java.getDeclaredMethod("contains", String::class.java)
        method.isAccessible = true
        return method.invoke(this, globalIdValue) as Boolean
    }

    private fun KafkaCdoSnapshotRepository.invokeGetSeq(commitId: CommitId): Long {
        val method = KafkaCdoSnapshotRepository::class.java.getDeclaredMethod("getSeq", CommitId::class.java)
        method.isAccessible = true
        return method.invoke(this, commitId) as Long
    }

    private fun KafkaCdoSnapshotRepository.invokeGetSnapshotSize(globalIdValue: String): Int {
        val method = KafkaCdoSnapshotRepository::class.java.getDeclaredMethod("getSnapshotSize", String::class.java)
        method.isAccessible = true
        return method.invoke(this, globalIdValue) as Int
    }

    private class InterruptedCompletableFuture: CompletableFuture<SendResult<String, String>>() {
        override fun get(timeout: Long, unit: java.util.concurrent.TimeUnit): SendResult<String, String> =
            throw InterruptedException("interrupted")
    }

    private class RecordingKafkaTemplate: KafkaTemplate<String, String>(KafkaProvider.producerFactory) {

        var recordedTopic: String? = null
            private set

        var recordedKey: String? = null
            private set

        var sendDefaultCount: Int = 0
            private set

        override fun send(topic: String, key: String, data: String?): CompletableFuture<SendResult<String, String>> {
            recordedTopic = topic
            recordedKey = key
            @Suppress("UNCHECKED_CAST")
            return CompletableFuture.completedFuture(null) as CompletableFuture<SendResult<String, String>>
        }

        override fun sendDefault(key: String, data: String?): CompletableFuture<SendResult<String, String>> {
            sendDefaultCount++
            @Suppress("UNCHECKED_CAST")
            return CompletableFuture.completedFuture(null) as CompletableFuture<SendResult<String, String>>
        }
    }
}
