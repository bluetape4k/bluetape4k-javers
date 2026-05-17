package io.bluetape4k.javers.persistence.kafka.repository

import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Write-only JaVers repository that publishes [CdoSnapshot] to a Kafka topic.
 *
 * ## Behavior / Contract
 * - [saveSnapshot] publishes to the default topic via [KafkaTemplate.sendDefault],
 *   using the GlobalId as the key and the encoded snapshot as the value.
 * - The publish blocks up to [publishTimeout] (default 30 s). A [java.util.concurrent.TimeoutException]
 *   is wrapped in [RuntimeException] and propagated so that [persist] does not advance the head.
 * - Publish failures are propagated as [RuntimeException] so that [persist] sees the failure
 *   and does not advance the audit-log head on error.
 * - **This repository is write-only.** All read methods ([getKeys], [contains], [getSeq],
 *   [getSnapshotSize], [loadSnapshots]) return empty/false/0 and log a warning on each call.
 *   Use a separate read-side repository (e.g. Redis, RDBMS) for query operations.
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
 * @property publishTimeout maximum time to wait for a Kafka publish acknowledgement (default 30 s)
 */
class KafkaCdoSnapshotRepository(
    private val kafkaOperations: KafkaTemplate<String, String>,
    private val publishTimeout: Duration = Duration.ofSeconds(30),
): AbstractCdoSnapshotRepository<String>(JaversCodecs.String) {

    companion object: KLogging()

    override fun getKeys(): Set<String> {
        log.warn { "KafkaCdoSnapshotRepository is write-only; getKeys() always returns empty" }
        return emptySet()
    }

    override fun contains(globalIdValue: String): Boolean {
        log.warn { "KafkaCdoSnapshotRepository is write-only; contains() always returns false" }
        return false
    }

    override fun getSeq(commitId: CommitId): Long {
        log.warn { "KafkaCdoSnapshotRepository is write-only; getSeq() always returns 0" }
        return 0L
    }

    override fun updateCommitId(commitId: CommitId, sequence: Long) {
        // Nothing to do — write-only repository does not track commit sequences.
    }

    override fun getSnapshotSize(globalIdValue: String): Int {
        log.warn { "KafkaCdoSnapshotRepository is write-only; getSnapshotSize() always returns 0" }
        return 0
    }

    override fun saveSnapshot(snapshot: CdoSnapshot) {
        val key = snapshot.globalId.value()
        val value = encode(snapshot)
        log.trace { "Produce snapshot. key=$key, value=$value" }
        try {
            kafkaOperations.sendDefault(key, value).get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Kafka publish interrupted for key=$key", e)
        } catch (e: Exception) {
            throw RuntimeException("Kafka publish failed for key=$key", e)
        }
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> {
        log.warn { "KafkaCdoSnapshotRepository is write-only; loadSnapshots() always returns empty" }
        return emptyList()
    }
}
