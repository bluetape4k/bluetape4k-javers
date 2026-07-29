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
 * [CdoSnapshot]을 Kafka topic에 publish하는 write-only JaVers repository입니다.
 *
 * ## 동작 / 계약
 * - [saveSnapshot]은 [topic]이 설정되어 있으면 해당 topic으로 publish하고,
 *   그렇지 않으면 [KafkaTemplate] default topic으로 publish합니다.
 *   GlobalId를 key로, encode된 snapshot을 value로 사용합니다.
 * - publish는 [publishTimeout]까지 block합니다(기본 30초). [java.util.concurrent.TimeoutException]은
 *   [RuntimeException]으로 wrap되어 전파되므로 [persist]가 head를 advance하지 않습니다.
 * - publish failure는 [RuntimeException]으로 전파되어 [persist]가 failure를 보고
 *   error 시 audit-log head를 advance하지 않습니다.
 * - **이 repository는 write-only입니다.** 모든 read method([getKeys], [contains], [getSeq],
 *   [getSnapshotSize], [loadSnapshots])는 empty/false/0을 반환합니다.
 *   첫 read-path 호출은 contract를 드러내기 위해 warning을 기록하고, 반복 호출은 debug level로 기록합니다.
 *   query operation에는 별도 read-side repository(예: Redis, RDBMS)를 사용하세요.
 * - codec은 [JaversCodecs.String](압축하지 않는 JSON string)입니다.
 *
 * ```kotlin
 * val repo = KafkaCdoSnapshotRepository(kafkaTemplate)
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(repo)
 *     .build()
 * // javers.commit("author", entity) → publishes snapshot to Kafka topic
 * ```
 *
 * @property kafkaOperations publish에 사용하는 [KafkaTemplate] instance입니다.
 * @property publishTimeout Kafka publish acknowledgement를 기다리는 최대 시간입니다(기본 30초).
 */
class KafkaCdoSnapshotRepository(
    private val kafkaOperations: KafkaTemplate<String, String>,
    private val publishTimeout: Duration = Duration.ofSeconds(30),
): AbstractCdoSnapshotRepository<String>(JaversCodecs.String) {

    /**
     * [topic]으로 publish하는 write-only repository를 생성합니다.
     */
    constructor(
        kafkaOperations: KafkaTemplate<String, String>,
        publishTimeout: Duration = Duration.ofSeconds(30),
        topic: String,
    ): this(kafkaOperations, publishTimeout) {
        publisher = KafkaSnapshotEventPublisher.withTopic(
            kafkaOperations = kafkaOperations,
            topic = topic,
            publishTimeout = publishTimeout.requirePositivePublishTimeout(),
        )
    }

    companion object: KLogging()

    private val readContractWarningLogged = atomic(false)
    private var publisher = KafkaSnapshotEventPublisher(kafkaOperations, publishTimeout.requirePositivePublishTimeout())

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
        // 수행할 작업이 없습니다. write-only repository는 commit sequence를 추적하지 않습니다.
    }

    override fun getSnapshotSize(globalIdValue: String): Int {
        logReadContract("getSnapshotSize()", "0")
        return 0
    }

    override fun saveSnapshot(snapshot: CdoSnapshot) {
        val key = snapshot.globalId.value()
        val event = snapshot.toSnapshotEvent()
        log.trace {
            "Produce snapshot. ${KafkaSnapshotKeyDiagnostics.format(key)}, " +
                "version=${event.metadata.snapshotVersion}, codec=${event.metadata.codecId}"
        }
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
