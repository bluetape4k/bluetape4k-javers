package io.bluetape4k.javers.persistence.kafka.projection

import io.bluetape4k.support.requireGt
import io.bluetape4k.support.requireNotBlank
import java.io.Serializable
import java.time.Duration

/**
 * [KafkaCdoSnapshotProjector] option입니다.
 *
 * ## 동작 / 계약
 * - [topic]은 encode된 JaVers snapshot payload를 담은 Kafka topic입니다.
 * - [pollTimeout]은 각 Kafka consumer poll 시간을 제한하며 positive여야 합니다.
 * - [subscribeOnStart]는 projector 생성 시 consumer를 [topic]에 subscribe합니다.
 * - projector는 source sequence가 wire에 없을 때 전역 head 정합성을 보장하기 위해
 *   [topic]이 정확히 하나의 Kafka partition을 갖는지 첫 poll 전에 검증합니다.
 * - [commitOffsetsAfterProjection]은 poll된 batch가 성공적으로 project된 뒤에만 offset을 commit합니다.
 * - [skipExistingSnapshots]는 target repository에 이미 존재하는 snapshot을 건너뜁니다.
 * - [closeConsumerOnClose]는 [KafkaCdoSnapshotProjector.close]가 consumer를 close할지 제어합니다.
 */
@ConsistentCopyVisibility
data class KafkaCdoSnapshotProjectionOptions private constructor(
    val topic: String,
    val pollTimeout: Duration = Duration.ofSeconds(1),
    val subscribeOnStart: Boolean = true,
    val commitOffsetsAfterProjection: Boolean = true,
    val skipExistingSnapshots: Boolean = true,
    val closeConsumerOnClose: Boolean = false,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = -7230065706952377972L

        /**
         * validation된 [KafkaCdoSnapshotProjector] option을 생성합니다.
         */
        operator fun invoke(
            topic: String,
            pollTimeout: Duration = Duration.ofSeconds(1),
            subscribeOnStart: Boolean = true,
            commitOffsetsAfterProjection: Boolean = true,
            skipExistingSnapshots: Boolean = true,
            closeConsumerOnClose: Boolean = false,
        ): KafkaCdoSnapshotProjectionOptions {
            topic.requireNotBlank("topic")
            pollTimeout.requireGt(Duration.ZERO, "pollTimeout")

            return KafkaCdoSnapshotProjectionOptions(
                topic = topic,
                pollTimeout = pollTimeout,
                subscribeOnStart = subscribeOnStart,
                commitOffsetsAfterProjection = commitOffsetsAfterProjection,
                skipExistingSnapshots = skipExistingSnapshots,
                closeConsumerOnClose = closeConsumerOnClose,
            )
        }
    }
}
