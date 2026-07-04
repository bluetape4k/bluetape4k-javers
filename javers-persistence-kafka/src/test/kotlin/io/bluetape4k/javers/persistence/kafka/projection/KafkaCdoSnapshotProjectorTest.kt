package io.bluetape4k.javers.persistence.kafka.projection

import com.google.gson.JsonObject
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.codec.Base58
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepository
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTable
import io.bluetape4k.javers.persistence.exposed.schema.CommitTable
import io.bluetape4k.javers.repository.caffeine.CaffeineCdoSnapshotRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.CloseOptions
import org.apache.kafka.common.TopicPartition
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.core.model.SnapshotEntity
import org.javers.repository.jql.QueryBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import java.time.Duration

class KafkaCdoSnapshotProjectorTest {

    @Test
    fun `options reject blank topic and non positive poll timeout`() {
        assertFailsWith<IllegalArgumentException> {
            KafkaCdoSnapshotProjectionOptions(topic = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            KafkaCdoSnapshotProjectionOptions(topic = "audit.snapshots", pollTimeout = Duration.ZERO)
        }
    }

    @Test
    fun `projectOnce decodes snapshot and saves to projection repository`() {
        val source = snapshotFixture()
        val targetRepository = CaffeineCdoSnapshotRepository()
        val targetJavers = newJavers(targetRepository)
        val consumer = mockConsumer(records(source.encodedSnapshot))
        val projector = KafkaCdoSnapshotProjector(
            consumer = consumer,
            jsonConverter = targetJavers.jsonConverter,
            projectionRepository = targetRepository,
            options = projectionOptions(),
        )

        val result = projector.projectOnce()

        result.polledRecords shouldBeEqualTo 1
        result.projectedSnapshots shouldBeEqualTo 1
        result.skippedSnapshots shouldBeEqualTo 0
        targetRepository.loadSnapshots(source.globalIdValue) shouldHaveSize 1
        targetRepository.getHeadId() shouldBeEqualTo source.snapshot.commitMetadata.id
        verify(exactly = 1) { consumer.commitSync() }
    }

    @Test
    fun `projectOnce restores head and newest first ordering`() {
        val source = snapshotStreamFixture()
        val targetRepository = CaffeineCdoSnapshotRepository()
        val targetJavers = newJavers(targetRepository)
        val consumer = mockConsumer(records(*source.encodedSnapshots.toTypedArray()))
        val projector = KafkaCdoSnapshotProjector(
            consumer = consumer,
            jsonConverter = targetJavers.jsonConverter,
            projectionRepository = targetRepository,
            options = projectionOptions(),
        )

        val result = projector.projectOnce()

        result.polledRecords shouldBeEqualTo 2
        result.projectedSnapshots shouldBeEqualTo 2
        targetRepository.getHeadId() shouldBeEqualTo source.headCommitId
        targetRepository.loadSnapshots(source.globalIdValue).map { it.version } shouldBeEqualTo listOf(2L, 1L)
        targetJavers.findSnapshots(QueryBuilder.byInstanceId(1, SnapshotEntity::class.java).build())
            .map { it.version } shouldBeEqualTo listOf(2L, 1L)
        verify(exactly = 1) { consumer.commitSync() }
    }

    @Test
    fun `projectOnce restores Exposed head and newest first ordering`() {
        val source = snapshotStreamFixture()
        val database = newExposedDatabase()
        try {
            val targetRepository = ExposedCdoSnapshotRepository(database)
            val targetJavers = JaversBuilder.javers()
                .registerJaversRepository(targetRepository)
                .build()
            val consumer = mockConsumer(records(*source.encodedSnapshots.toTypedArray()))
            val projector = KafkaCdoSnapshotProjector(
                consumer = consumer,
                jsonConverter = targetJavers.jsonConverter,
                projectionRepository = targetRepository,
                options = projectionOptions(),
            )

            val result = projector.projectOnce()

            result.polledRecords shouldBeEqualTo 2
            result.projectedSnapshots shouldBeEqualTo 2
            targetRepository.getHeadId() shouldBeEqualTo source.headCommitId
            targetRepository.loadSnapshots(source.globalIdValue).map { it.version } shouldBeEqualTo listOf(2L, 1L)
            targetJavers.findSnapshots(QueryBuilder.byInstanceId(1, SnapshotEntity::class.java).build())
                .map { it.version } shouldBeEqualTo listOf(2L, 1L)
            verify(exactly = 1) { consumer.commitSync() }
        } finally {
            transaction(database) {
                SchemaUtils.drop(CdoSnapshotTable, CommitTable)
            }
        }
    }

    @Test
    fun `projectOnce skips existing snapshot for idempotent replay`() {
        val source = snapshotFixture()
        val targetRepository = CaffeineCdoSnapshotRepository()
        val targetJavers = newJavers(targetRepository)
        targetRepository.saveSnapshot(source.snapshot)
        val consumer = mockConsumer(records(source.encodedSnapshot))
        val projector = KafkaCdoSnapshotProjector(
            consumer = consumer,
            jsonConverter = targetJavers.jsonConverter,
            projectionRepository = targetRepository,
            options = projectionOptions(),
        )

        val result = projector.projectOnce()

        result.polledRecords shouldBeEqualTo 1
        result.projectedSnapshots shouldBeEqualTo 0
        result.skippedSnapshots shouldBeEqualTo 1
        targetRepository.loadSnapshots(source.globalIdValue) shouldHaveSize 1
        verify(exactly = 1) { consumer.commitSync() }
    }

    @Test
    fun `projectOnce does not commit offsets when decoding fails`() {
        val targetRepository = CaffeineCdoSnapshotRepository()
        val targetJavers = newJavers(targetRepository)
        val consumer = mockConsumer(records("not-json"))
        val projector = KafkaCdoSnapshotProjector(
            consumer = consumer,
            jsonConverter = targetJavers.jsonConverter,
            projectionRepository = targetRepository,
            options = projectionOptions(),
        )

        assertFailsWith<IllegalArgumentException> {
            projector.projectOnce()
        }
        verify(exactly = 0) { consumer.commitSync() }
    }

    @Test
    fun `close closes consumer only when projector owns it`() {
        val targetRepository = CaffeineCdoSnapshotRepository()
        val targetJavers = newJavers(targetRepository)
        val consumer = mockConsumer(emptyRecords())
        val options = KafkaCdoSnapshotProjectionOptions(
            topic = "audit.snapshots",
            subscribeOnStart = false,
            closeConsumerOnClose = true,
        )
        val projector = KafkaCdoSnapshotProjector(
            consumer = consumer,
            jsonConverter = targetJavers.jsonConverter,
            projectionRepository = targetRepository,
            options = options,
        )

        projector.close()

        verify(exactly = 1) { consumer.close(any<CloseOptions>()) }
    }

    @Test
    fun `replayUntilIdle validates idle poll count`() {
        val targetRepository = CaffeineCdoSnapshotRepository()
        val targetJavers = newJavers(targetRepository)
        val projector = KafkaCdoSnapshotProjector(
            consumer = mockConsumer(emptyRecords()),
            jsonConverter = targetJavers.jsonConverter,
            projectionRepository = targetRepository,
            options = projectionOptions(),
        )

        assertFailsWith<IllegalArgumentException> {
            projector.replayUntilIdle(maxIdlePolls = 0)
        }
    }

    private fun snapshotFixture(): SnapshotFixture {
        val repository = CaffeineCdoSnapshotRepository(JaversCodecs.String)
        val javers = newJavers(repository)
        val snapshot = javers.commit("projection", SnapshotEntity(1).apply { intProperty = 7 }).snapshots.first()
        val encoded = JaversCodecs.String.encode(javers.jsonConverter.toJsonElement(snapshot) as JsonObject)
        return SnapshotFixture(
            snapshot = snapshot,
            encodedSnapshot = encoded,
            globalIdValue = snapshot.globalId.value(),
        )
    }

    private fun snapshotStreamFixture(): SnapshotStreamFixture {
        val repository = CaffeineCdoSnapshotRepository(JaversCodecs.String)
        val javers = newJavers(repository)
        val entity = SnapshotEntity(1).apply { intProperty = 7 }
        val firstSnapshot = javers.commit("projection", entity).snapshots.first()
        entity.intProperty = 11
        val secondCommit = javers.commit("projection", entity)
        val secondSnapshot = secondCommit.snapshots.first()

        return SnapshotStreamFixture(
            encodedSnapshots = listOf(firstSnapshot, secondSnapshot).map { snapshot ->
                JaversCodecs.String.encode(javers.jsonConverter.toJsonElement(snapshot) as JsonObject)
            },
            globalIdValue = firstSnapshot.globalId.value(),
            headCommitId = secondCommit.id,
        )
    }

    private fun newJavers(repository: CaffeineCdoSnapshotRepository): Javers =
        JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()

    private fun mockConsumer(
        records: ConsumerRecords<String, String>,
    ): Consumer<String, String> {
        val consumer = mockk<Consumer<String, String>>(relaxed = true)
        every { consumer.poll(any<Duration>()) } returns records
        every { consumer.commitSync() } just Runs
        every { consumer.close(any<CloseOptions>()) } just Runs
        return consumer
    }

    private fun records(vararg values: String): ConsumerRecords<String, String> {
        val topicPartition = TopicPartition("audit.snapshots", 0)
        val records = values.mapIndexed { index, value ->
            ConsumerRecord("audit.snapshots", 0, index.toLong(), "snapshot-key-$index", value)
        }
        return ConsumerRecords(mapOf(topicPartition to records), emptyMap())
    }

    private fun emptyRecords(): ConsumerRecords<String, String> = ConsumerRecords.empty()

    private fun projectionOptions(): KafkaCdoSnapshotProjectionOptions =
        KafkaCdoSnapshotProjectionOptions(
            topic = "audit.snapshots",
            subscribeOnStart = false,
        )

    private data class SnapshotFixture(
        val snapshot: CdoSnapshot,
        val encodedSnapshot: String,
        val globalIdValue: String,
    )

    private data class SnapshotStreamFixture(
        val encodedSnapshots: List<String>,
        val globalIdValue: String,
        val headCommitId: CommitId,
    )

    private fun newExposedDatabase(): Database {
        val database = Database.connect(
            url = "jdbc:h2:mem:javers-kafka-projection-${Base58.randomString(8)};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
        transaction(database) {
            SchemaUtils.create(CommitTable, CdoSnapshotTable)
        }
        return database
    }
}
