package io.bluetape4k.javers.persistence.kafka.repository

import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import kotlinx.atomicfu.atomic
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import java.io.Serializable
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Options for [VanillaKafkaCdoSnapshotRepository].
 *
 * ## Behavior / Contract
 * - [topic] is the Kafka topic used for every published snapshot.
 * - [publishTimeout] bounds the blocking wait for Kafka acknowledgement.
 * - [flushAfterSend] calls [Producer.flush] after a successful acknowledgement.
 * - [closeProducerOnClose] controls whether [VanillaKafkaCdoSnapshotRepository.close]
 *   closes the producer. It defaults to `false` because producers are usually
 *   owned by the application lifecycle.
 */
data class VanillaKafkaCdoSnapshotRepositoryOptions(
    val topic: String,
    val publishTimeout: Duration = Duration.ofSeconds(30),
    val flushAfterSend: Boolean = false,
    val closeProducerOnClose: Boolean = false,
): Serializable {

    init {
        topic.requireNotBlank("topic")
        require(!publishTimeout.isZero && !publishTimeout.isNegative) {
            "publishTimeout must be positive: $publishTimeout"
        }
    }

    companion object {
        private const val serialVersionUID: Long = 1532863228759821530L
    }
}

/**
 * Write-only JaVers repository that publishes [CdoSnapshot] values with a vanilla Kafka [Producer].
 *
 * ## Behavior / Contract
 * - [saveSnapshot] publishes a [ProducerRecord] to [options.topic], using [keyMapper]
 *   for the key and the encoded snapshot as the value.
 * - The publish blocks up to [VanillaKafkaCdoSnapshotRepositoryOptions.publishTimeout].
 * - Publish failures are propagated as [RuntimeException] so that [persist] does not
 *   advance the audit-log head on error.
 * - [InterruptedException] restores the thread interrupt flag before propagation.
 * - **This repository is write-only.** All read methods return empty/false/0.
 *   The first read-path call logs a warning; repeated read-path calls log at debug level.
 * - The producer is caller-owned by default. Set [VanillaKafkaCdoSnapshotRepositoryOptions.closeProducerOnClose]
 *   to `true` when this repository should close it.
 *
 * ```kotlin
 * val options = VanillaKafkaCdoSnapshotRepositoryOptions(topic = "order-audit-events")
 * val repo = VanillaKafkaCdoSnapshotRepository(producer, options)
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(repo)
 *     .build()
 * ```
 *
 * @property producer the Apache Kafka producer used for publishing snapshots
 * @property options publishing and lifecycle options
 * @property keyMapper maps a JaVers snapshot to the Kafka record key
 */
class VanillaKafkaCdoSnapshotRepository(
    private val producer: Producer<String, String>,
    private val options: VanillaKafkaCdoSnapshotRepositoryOptions,
    private val keyMapper: (CdoSnapshot) -> String = { it.globalId.value() },
): AbstractCdoSnapshotRepository<String>(JaversCodecs.String), AutoCloseable {

    companion object: KLogging()

    private val readContractWarningLogged = atomic(false)

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
        // Nothing to do: the Kafka publisher is write-only and does not persist commit sequences.
    }

    override fun getSnapshotSize(globalIdValue: String): Int {
        logReadContract("getSnapshotSize()", "0")
        return 0
    }

    override fun saveSnapshot(snapshot: CdoSnapshot) {
        val key = keyMapper(snapshot)
        val value = encode(snapshot)
        val record = ProducerRecord(options.topic, key, value)
        log.trace { "Produce snapshot. topic=${options.topic}, key=$key, value=$value" }

        try {
            producer.send(record).get(options.publishTimeout.toMillis(), TimeUnit.MILLISECONDS)
            if (options.flushAfterSend) {
                producer.flush()
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Kafka publish interrupted for topic=${options.topic}, key=$key", e)
        } catch (e: Exception) {
            throw RuntimeException("Kafka publish failed for topic=${options.topic}, key=$key", e)
        }
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> {
        logReadContract("loadSnapshots()", "empty")
        return emptyList()
    }

    override fun close() {
        if (options.closeProducerOnClose) {
            producer.close(options.publishTimeout)
        }
    }

    private fun logReadContract(operation: String, result: String) {
        val message = "VanillaKafkaCdoSnapshotRepository is write-only; $operation always returns $result"
        if (readContractWarningLogged.compareAndSet(expect = false, update = true)) {
            log.warn { message }
        } else {
            log.debug { message }
        }
    }
}

