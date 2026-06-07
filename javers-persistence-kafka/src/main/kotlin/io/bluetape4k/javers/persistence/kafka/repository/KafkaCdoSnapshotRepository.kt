package io.bluetape4k.javers.persistence.kafka.repository

import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.javers.repository.event.CdoSnapshotEvent
import io.bluetape4k.javers.repository.event.CdoSnapshotEventCodecIds
import io.bluetape4k.javers.repository.event.CdoSnapshotEventMetadata
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import kotlinx.atomicfu.atomic
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration

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
 *   [getSnapshotSize], [loadSnapshots]) return empty/false/0. The first read-path call logs
 *   a warning to make the contract visible; repeated read-path calls log at debug level.
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

    private val readContractWarningLogged = atomic(false)
    private val publisher = KafkaSnapshotEventPublisher(kafkaOperations, publishTimeout)

    override fun getKeys(): Set<String> {
        logReadContract("getKeys()", "empty")
        return emptySet()
    }

    override fun contains(globalIdValue: String): Boolean {
        logReadContract("contains()", "false")
        return false
    }

    override fun getSeq(commitId: CommitId): Long {
        logReadContract("getSeq()", "0")
        return 0L
    }

    override fun updateCommitId(commitId: CommitId, sequence: Long) {
        // Nothing to do — write-only repository does not track commit sequences.
    }

    override fun getSnapshotSize(globalIdValue: String): Int {
        logReadContract("getSnapshotSize()", "0")
        return 0
    }

    override fun saveSnapshot(snapshot: CdoSnapshot) {
        val key = snapshot.globalId.value()
        val event = snapshot.toSnapshotEvent()
        log.trace { "Produce snapshot. key=$key, value=${event.payload}" }
        publisher.publish(event, key)
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> {
        logReadContract("loadSnapshots()", "empty")
        return emptyList()
    }

    private fun logReadContract(operation: String, result: String) {
        val message = "KafkaCdoSnapshotRepository is write-only; $operation always returns $result"
        if (readContractWarningLogged.compareAndSet(expect = false, update = true)) {
            log.warn { message }
        } else {
            log.debug { message }
        }
    }

    private fun CdoSnapshot.toSnapshotEvent(): CdoSnapshotEvent<String> =
        CdoSnapshotEvent(
            metadata = CdoSnapshotEventMetadata.from(this, CdoSnapshotEventCodecIds.JSON_STRING),
            payload = encode(this),
        )
}
