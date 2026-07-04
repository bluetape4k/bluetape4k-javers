package io.bluetape4k.javers.persistence.kafka.projection

import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.persistence.kafka.repository.KafkaSnapshotKeyDiagnostics
import io.bluetape4k.javers.repository.CdoSnapshotRepository
import io.bluetape4k.kafka.consumerOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requirePositiveNumber
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.CloseOptions
import org.apache.kafka.common.serialization.StringDeserializer
import org.javers.core.json.JsonConverter
import org.javers.core.metamodel.`object`.CdoSnapshot
import java.util.Properties

/**
 * Projects Kafka snapshot records into a read-capable [CdoSnapshotRepository].
 *
 * ## Behavior / Contract
 * - Consumes Kafka records whose value is the encoded JaVers snapshot payload
 *   produced by `KafkaCdoSnapshotRepository` or `VanillaKafkaCdoSnapshotRepository`.
 * - Decodes each payload with [JaversCodecs.String] and [jsonConverter].
 * - Projects decoded snapshots into [projectionRepository] so repositories
 *   that track commit metadata can restore head and sequence state.
 * - Skips already projected snapshots when
 *   [KafkaCdoSnapshotProjectionOptions.skipExistingSnapshots] is `true`.
 * - Commits Kafka offsets only after the complete polled batch is projected
 *   when [KafkaCdoSnapshotProjectionOptions.commitOffsetsAfterProjection] is `true`.
 *
 * ```kotlin
 * val readRepository = CaffeineCdoSnapshotRepository()
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(readRepository)
 *     .build()
 *
 * val projector = KafkaCdoSnapshotProjector(
 *     consumerConfigs = consumerConfigs,
 *     jsonConverter = javers.jsonConverter,
 *     projectionRepository = readRepository,
 *     options = KafkaCdoSnapshotProjectionOptions(topic = "order-audit-events"),
 * )
 * projector.replayUntilIdle()
 * ```
 */
class KafkaCdoSnapshotProjector private constructor(
    private val consumer: Consumer<String, String>,
    private val jsonConverter: JsonConverter,
    private val projectionRepository: CdoSnapshotRepository,
    private val options: KafkaCdoSnapshotProjectionOptions,
): AutoCloseable {

    companion object: KLogging() {
        /**
         * Creates a projector with a caller-owned Kafka [Consumer].
         */
        operator fun invoke(
            consumer: Consumer<String, String>,
            jsonConverter: JsonConverter,
            projectionRepository: CdoSnapshotRepository,
            options: KafkaCdoSnapshotProjectionOptions,
        ): KafkaCdoSnapshotProjector =
            KafkaCdoSnapshotProjector(
                consumer = consumer,
                jsonConverter = jsonConverter,
                projectionRepository = projectionRepository,
                options = options,
            )

        /**
         * Creates a projector and repository-owned consumer with bluetape4k-kafka [consumerOf].
         */
        operator fun invoke(
            consumerConfigs: Map<String, Any?>,
            jsonConverter: JsonConverter,
            projectionRepository: CdoSnapshotRepository,
            options: KafkaCdoSnapshotProjectionOptions,
        ): KafkaCdoSnapshotProjector =
            KafkaCdoSnapshotProjector(
                consumer = consumerOf(
                    configs = consumerConfigs,
                    keyDeserializer = StringDeserializer(),
                    valueDeserializer = StringDeserializer(),
                ),
                jsonConverter = jsonConverter,
                projectionRepository = projectionRepository,
                options = options.asConsumerOwned(),
            )

        /**
         * Creates a projector and repository-owned consumer with bluetape4k-kafka [consumerOf].
         */
        operator fun invoke(
            consumerProperties: Properties,
            jsonConverter: JsonConverter,
            projectionRepository: CdoSnapshotRepository,
            options: KafkaCdoSnapshotProjectionOptions,
        ): KafkaCdoSnapshotProjector =
            KafkaCdoSnapshotProjector(
                consumer = consumerOf(
                    props = consumerProperties,
                    keyDeserializer = StringDeserializer(),
                    valueDeserializer = StringDeserializer(),
                ),
                jsonConverter = jsonConverter,
                projectionRepository = projectionRepository,
                options = options.asConsumerOwned(),
            )

        private fun KafkaCdoSnapshotProjectionOptions.asConsumerOwned(): KafkaCdoSnapshotProjectionOptions =
            KafkaCdoSnapshotProjectionOptions(
                topic = topic,
                pollTimeout = pollTimeout,
                subscribeOnStart = subscribeOnStart,
                commitOffsetsAfterProjection = commitOffsetsAfterProjection,
                skipExistingSnapshots = skipExistingSnapshots,
                closeConsumerOnClose = true,
            )
    }

    init {
        if (options.subscribeOnStart) {
            consumer.subscribe(listOf(options.topic))
        }
    }

    /**
     * Polls Kafka once and projects every returned snapshot record.
     */
    fun projectOnce(): KafkaCdoSnapshotProjectionResult {
        val records = consumer.poll(options.pollTimeout)
        var projected = 0
        var skipped = 0

        records
            .asSequence()
            .sortedWith(compareBy<ConsumerRecord<String, String>> { it.partition() }.thenBy { it.offset() })
            .forEach { record ->
                val snapshot = record.decodeSnapshot()
                if (options.skipExistingSnapshots && projectionRepository.containsSnapshot(snapshot)) {
                    skipped++
                    log.debug { "Skip existing projected snapshot. ${record.recordDiagnostics()}" }
                    return@forEach
                }

                projectionRepository.projectSnapshot(snapshot)
                projected++
            }

        if (!records.isEmpty && options.commitOffsetsAfterProjection) {
            consumer.commitSync()
        }

        return KafkaCdoSnapshotProjectionResult(
            polledRecords = records.count(),
            projectedSnapshots = projected,
            skippedSnapshots = skipped,
        )
    }

    /**
     * Replays records until [maxIdlePolls] consecutive polls return no records.
     */
    fun replayUntilIdle(maxIdlePolls: Int = 1): KafkaCdoSnapshotProjectionResult {
        maxIdlePolls.requirePositiveNumber("maxIdlePolls")

        var idlePolls = 0
        var total = KafkaCdoSnapshotProjectionResult()
        while (idlePolls < maxIdlePolls) {
            val result = projectOnce()
            total += result
            if (result.polledRecords == 0) {
                idlePolls++
            } else {
                idlePolls = 0
            }
        }
        return total
    }

    override fun close() {
        if (options.closeConsumerOnClose) {
            consumer.close(CloseOptions.timeout(options.pollTimeout))
        }
    }

    private fun ConsumerRecord<String, String>.decodeSnapshot(): CdoSnapshot {
        val jsonObject = runCatching { JaversCodecs.String.decode(value()) }
            .getOrElse { e -> throw IllegalArgumentException("Failed to decode Kafka snapshot record. ${recordDiagnostics()}", e) }
            ?: throw IllegalArgumentException("Failed to decode Kafka snapshot record. ${recordDiagnostics()}")

        return jsonConverter.fromJson(jsonObject, CdoSnapshot::class.java)
    }

    private fun CdoSnapshotRepository.containsSnapshot(snapshot: CdoSnapshot): Boolean {
        return loadSnapshots(snapshot.globalId.value()).any { existing ->
            existing.commitMetadata.id == snapshot.commitMetadata.id &&
                existing.version == snapshot.version
        }
    }

    private fun ConsumerRecord<String, String>.recordDiagnostics(): String {
        val keyDiagnostics = key()?.let { KafkaSnapshotKeyDiagnostics.format(it) }
            ?: "keyFingerprint=<null>, keyLength=0"
        return "topic=${topic()}, partition=${partition()}, offset=${offset()}, $keyDiagnostics"
    }
}
