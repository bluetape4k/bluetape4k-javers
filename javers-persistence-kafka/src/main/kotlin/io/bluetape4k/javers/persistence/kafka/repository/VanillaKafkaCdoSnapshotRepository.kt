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
 * [VanillaKafkaCdoSnapshotRepository] option입니다.
 *
 * ## 동작 / 계약
 * - [topic]은 publish되는 모든 snapshot에 사용하는 Kafka topic입니다.
 * - [publishTimeout]은 Kafka acknowledgement를 기다리는 blocking wait를 제한합니다.
 * - [flushAfterSend]는 acknowledgement가 성공한 뒤 [Producer.flush]를 호출합니다.
 * - [closeProducerOnClose]는 [VanillaKafkaCdoSnapshotRepository.close]가 producer를 close할지 제어합니다.
 *   producer는 보통 application lifecycle이 소유하므로 기본값은 `false`입니다.
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
         * validation된 [VanillaKafkaCdoSnapshotRepository] option을 생성합니다.
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
 * vanilla Kafka [Producer]로 [CdoSnapshot] 값을 publish하는 write-only JaVers repository입니다.
 *
 * ## 동작 / 계약
 * - [saveSnapshot]은 [options.topic]으로 Kafka record를 publish하며,
 *   key에는 [keyMapper], value에는 encode된 snapshot event payload를 사용합니다.
 * - publish는 [VanillaKafkaCdoSnapshotRepositoryOptions.publishTimeout]까지 block합니다.
 * - publish failure는 [RuntimeException]으로 전파되므로 [persist]가 error 시 audit-log head를 advance하지 않습니다.
 * - [InterruptedException]은 전파 전에 thread interrupt flag를 복원합니다.
 * - **이 repository는 write-only입니다.** 모든 read method는 empty/false/0을 반환합니다.
 *   첫 read-path 호출은 warning을 기록하고, 반복 read-path 호출은 debug level로 기록합니다.
 * - producer는 기본적으로 caller-owned입니다. 이 repository가 producer를 close해야 하면
 *   [VanillaKafkaCdoSnapshotRepositoryOptions.closeProducerOnClose]를 `true`로 설정하세요.
 *
 * ```kotlin
 * val options = VanillaKafkaCdoSnapshotRepositoryOptions(topic = "order-audit-events")
 * val repo = VanillaKafkaCdoSnapshotRepository(producerConfigs, options)
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(repo)
 *     .build()
 * ```
 *
 * @property producer snapshot publish에 사용하는 Apache Kafka producer입니다.
 * @property options publish 및 lifecycle option입니다.
 * @property keyMapper JaVers snapshot을 Kafka record key로 mapping합니다.
 */
class VanillaKafkaCdoSnapshotRepository private constructor(
    private val producer: Producer<String, String>,
    private val options: VanillaKafkaCdoSnapshotRepositoryOptions,
    private val keyMapper: (CdoSnapshot) -> String = { it.globalId.value() },
): AbstractCdoSnapshotRepository<String>(JaversCodecs.String), AutoCloseable {

    companion object: KLogging() {
        /**
         * Apache Kafka [Producer] 기반 write-only JaVers repository를 생성합니다.
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
         * bluetape4k-kafka [producerOf]로 repository와 Kafka producer를 생성합니다.
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
         * bluetape4k-kafka [producerOf]로 repository와 Kafka producer를 생성합니다.
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
        // 수행할 작업이 없습니다. Kafka publisher는 write-only이며 commit sequence를 persist하지 않습니다.
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
