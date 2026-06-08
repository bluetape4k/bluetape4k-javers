package io.bluetape4k.javers.persistence.kafka.repository

import io.bluetape4k.kafka.producerOf
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.javers.repository.event.CdoSnapshotEvent
import io.bluetape4k.javers.repository.event.CdoSnapshotEventCodecIds
import io.bluetape4k.javers.repository.event.CdoSnapshotEventMetadata
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import kotlinx.atomicfu.atomic
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.StringSerializer
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import java.io.Serializable
import java.time.Duration
import java.util.Properties

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
@ConsistentCopyVisibility
data class VanillaKafkaCdoSnapshotRepositoryOptions private constructor(
    val topic: String,
    val publishTimeout: Duration = Duration.ofSeconds(30),
    val flushAfterSend: Boolean = false,
    val closeProducerOnClose: Boolean = false,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1532863228759821530L

        /**
         * Creates validated options for [VanillaKafkaCdoSnapshotRepository].
         */
        operator fun invoke(
            topic: String,
            publishTimeout: Duration = Duration.ofSeconds(30),
            flushAfterSend: Boolean = false,
            closeProducerOnClose: Boolean = false,
        ): VanillaKafkaCdoSnapshotRepositoryOptions {
            topic.requireNotBlank("topic")
            publishTimeout.requirePositivePublishTimeout()

            return VanillaKafkaCdoSnapshotRepositoryOptions(
                topic = topic,
                publishTimeout = publishTimeout,
                flushAfterSend = flushAfterSend,
                closeProducerOnClose = closeProducerOnClose,
            )
        }
    }
}

/**
 * Write-only JaVers repository that publishes [CdoSnapshot] values with a vanilla Kafka [Producer].
 *
 * ## Behavior / Contract
 * - [saveSnapshot] publishes a Kafka record to [options.topic], using [keyMapper]
 *   for the key and the encoded snapshot event payload as the value.
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
 * val repo = VanillaKafkaCdoSnapshotRepository(producerConfigs, options)
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(repo)
 *     .build()
 * ```
 *
 * @property producer the Apache Kafka producer used for publishing snapshots
 * @property options publishing and lifecycle options
 * @property keyMapper maps a JaVers snapshot to the Kafka record key
 */
class VanillaKafkaCdoSnapshotRepository private constructor(
    private val producer: Producer<String, String>,
    private val options: VanillaKafkaCdoSnapshotRepositoryOptions,
    private val keyMapper: (CdoSnapshot) -> String = { it.globalId.value() },
): AbstractCdoSnapshotRepository<String>(JaversCodecs.String), AutoCloseable {

    companion object: KLogging() {
        /**
         * Creates a write-only JaVers repository backed by an Apache Kafka [Producer].
         */
        operator fun invoke(
            producer: Producer<String, String>,
            options: VanillaKafkaCdoSnapshotRepositoryOptions,
            keyMapper: (CdoSnapshot) -> String = { it.globalId.value() },
        ): VanillaKafkaCdoSnapshotRepository =
            VanillaKafkaCdoSnapshotRepository(
                producer = producer,
                options = options,
                keyMapper = keyMapper,
            )

        /**
         * Creates a repository and its Kafka producer with bluetape4k-kafka [producerOf].
         */
        operator fun invoke(
            producerConfigs: Map<String, Any?>,
            options: VanillaKafkaCdoSnapshotRepositoryOptions,
            keyMapper: (CdoSnapshot) -> String = { it.globalId.value() },
        ): VanillaKafkaCdoSnapshotRepository =
            VanillaKafkaCdoSnapshotRepository(
                producer = producerOf(
                    configs = producerConfigs,
                    keySerializer = StringSerializer(),
                    valueSerializer = StringSerializer(),
                ),
                options = options.asRepositoryOwned(),
                keyMapper = keyMapper,
            )

        /**
         * Creates a repository and its Kafka producer with bluetape4k-kafka [producerOf].
         */
        operator fun invoke(
            producerProperties: Properties,
            options: VanillaKafkaCdoSnapshotRepositoryOptions,
            keyMapper: (CdoSnapshot) -> String = { it.globalId.value() },
        ): VanillaKafkaCdoSnapshotRepository =
            VanillaKafkaCdoSnapshotRepository(
                producer = producerOf(
                    props = producerProperties,
                    keySerializer = StringSerializer(),
                    valueSerializer = StringSerializer(),
                ),
                options = options.asRepositoryOwned(),
                keyMapper = keyMapper,
            )

        private fun VanillaKafkaCdoSnapshotRepositoryOptions.asRepositoryOwned(): VanillaKafkaCdoSnapshotRepositoryOptions =
            VanillaKafkaCdoSnapshotRepositoryOptions(
                topic = topic,
                publishTimeout = publishTimeout,
                flushAfterSend = flushAfterSend,
                closeProducerOnClose = true,
            )
    }

    private val readContractWarningLogged = atomic(false)
    private val publisher = VanillaKafkaSnapshotEventPublisher(producer, options)

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
        val event = snapshot.toSnapshotEvent()
        log.trace {
            "Produce snapshot. topic=${options.topic}, ${KafkaSnapshotKeyDiagnostics.format(key)}, " +
                "version=${event.metadata.snapshotVersion}, codec=${event.metadata.codecId}"
        }
        publisher.publish(event, key)
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> {
        logReadContract("loadSnapshots()", "empty")
        return emptyList()
    }

    override fun close() {
        publisher.close()
    }

    private fun logReadContract(operation: String, result: String) {
        val message = "VanillaKafkaCdoSnapshotRepository is write-only; $operation always returns $result"
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
