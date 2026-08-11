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
 * Kafka snapshot record를 read-capable [CdoSnapshotRepository]로 project합니다.
 *
 * ## 동작 / 계약
 * - `KafkaCdoSnapshotRepository` 또는 `VanillaKafkaCdoSnapshotRepository`가 생성한
 *   encode된 JaVers snapshot payload를 value로 가진 Kafka record를 consume합니다.
 * - 각 payload를 [JaversCodecs.String]과 [jsonConverter]로 decode합니다.
 * - commit metadata를 추적하는 repository가 head 및 sequence state를 복원할 수 있도록
 *   decode된 snapshot을 [projectionRepository]로 project합니다.
 * - source repository sequence가 현재 Kafka wire value에 포함되지 않으므로,
 *   전역 head 정합성을 위해 topic이 정확히 하나의 partition을 갖는지 첫 poll 전에 검증합니다.
 * - [KafkaCdoSnapshotProjectionOptions.skipExistingSnapshots]가 `true`이면 이미 project된 snapshot을 건너뜁니다.
 * - [KafkaCdoSnapshotProjectionOptions.commitOffsetsAfterProjection]이 `true`이면
 *   poll된 전체 batch가 project된 뒤에만 Kafka offset을 commit합니다.
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

    private var topicTopologyValidated = false

    companion object: KLogging() {
        /**
         * caller-owned Kafka [Consumer]로 projector를 생성합니다.
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
         * bluetape4k-kafka [consumerOf]로 projector와 repository-owned consumer를 생성합니다.
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
         * bluetape4k-kafka [consumerOf]로 projector와 repository-owned consumer를 생성합니다.
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
     * Kafka를 한 번 poll하고 반환된 모든 snapshot record를 project합니다.
     */
    fun projectOnce(): KafkaCdoSnapshotProjectionResult {
        validateTopicTopology()
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

    private fun validateTopicTopology() {
        if (topicTopologyValidated) {
            return
        }

        val partitionCount = consumer.partitionsFor(options.topic).size
        check(partitionCount == 1) {
            "Kafka snapshot projection requires a single-partition topic. " +
                "topic=${options.topic}, partitions=$partitionCount"
        }
        topicTopologyValidated = true
    }

    /**
     * [maxIdlePolls]번 연속 poll에서 record가 반환되지 않을 때까지 record를 반복 처리합니다.
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
