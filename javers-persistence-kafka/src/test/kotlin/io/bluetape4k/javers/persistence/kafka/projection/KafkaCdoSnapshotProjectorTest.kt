package io.bluetape4k.javers.persistence.kafka.projection

import com.google.gson.JsonObject
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.javers.codecs.JaversCodecs
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
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.core.model.SnapshotEntity
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
        targetRepository.loadSnapshots(source.globalIdValue).size shouldBeEqualTo 1
        verify(exactly = 1) { consumer.commitSync() }
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
        targetRepository.loadSnapshots(source.globalIdValue).size shouldBeEqualTo 1
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

    private fun records(value: String): ConsumerRecords<String, String> {
        val topicPartition = TopicPartition("audit.snapshots", 0)
        val record = ConsumerRecord("audit.snapshots", 0, 0L, "snapshot-key", value)
        return ConsumerRecords(mapOf(topicPartition to listOf(record)), emptyMap())
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
}
