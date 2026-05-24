package io.bluetape4k.javers.persistence.kafka.repository

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.logging.KLogging
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.`object`.SnapshotType
import org.javers.core.model.PrimitiveEntity
import org.javers.core.model.SnapshotEntity
import org.javers.core.repository.AbstractJaversCommitTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
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
}
