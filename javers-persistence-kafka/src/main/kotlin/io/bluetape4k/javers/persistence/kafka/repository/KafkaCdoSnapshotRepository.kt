package io.bluetape4k.javers.persistence.kafka.repository

import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.springframework.kafka.core.KafkaTemplate

/**
 * Write-only JaVers repository that publishes [CdoSnapshot] to a Kafka topic.
 *
 * ## Behavior / Contract
 * - [saveSnapshot] publishes to the default topic via [KafkaTemplate.sendDefault],
 *   using the GlobalId as the key and the encoded snapshot as the value.
 * - Publish failures are propagated as [RuntimeException] so that [persist] sees the failure
 *   and does not advance the audit-log head on error.
 * - Read methods ([loadSnapshots], [getKeys], etc.) always return empty collections or zero.
 * - The codec is [JaversCodecs.String] (uncompressed JSON string).
 *
 * ```kotlin
 * val repo = KafkaCdoSnapshotRepository(kafkaTemplate)
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(repo)
 *     .build()
 * // javers.commit("author", entity) → publishes snapshot to Kafka topic
 * ```
 *
 * @property kafkaOperations the [KafkaTemplate] instance used for publishing
 */
class KafkaCdoSnapshotRepository(
    private val kafkaOperations: KafkaTemplate<String, String>,
): AbstractCdoSnapshotRepository<String>(JaversCodecs.String) {

    companion object: KLogging()

    override fun getKeys(): Set<String> = emptySet()

    override fun contains(globalIdValue: String): Boolean = false

    override fun getSeq(commitId: CommitId): Long = 0L

    override fun updateCommitId(commitId: CommitId, sequence: Long) {
        // Nothing to do.
    }

    override fun getSnapshotSize(globalIdValue: String): Int = 0

    override fun saveSnapshot(snapshot: CdoSnapshot) {
        val key = snapshot.globalId.value()
        val value = encode(snapshot)
        log.trace { "Produce snapshot. key=$key, value=$value" }
        try {
            kafkaOperations.sendDefault(key, value).get()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Kafka publish interrupted for key=$key", e)
        } catch (e: Exception) {
            throw RuntimeException("Kafka publish failed for key=$key", e)
        }
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> = emptyList()
}
