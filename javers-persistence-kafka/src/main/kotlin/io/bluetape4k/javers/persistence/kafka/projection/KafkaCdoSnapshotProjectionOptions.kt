package io.bluetape4k.javers.persistence.kafka.projection

import io.bluetape4k.support.requireGt
import io.bluetape4k.support.requireNotBlank
import java.io.Serializable
import java.time.Duration

/**
 * Options for [KafkaCdoSnapshotProjector].
 *
 * ## Behavior / Contract
 * - [topic] is the Kafka topic that contains encoded JaVers snapshot payloads.
 * - [pollTimeout] bounds each Kafka consumer poll and must be positive.
 * - [subscribeOnStart] subscribes the consumer to [topic] when the projector is created.
 * - [commitOffsetsAfterProjection] commits offsets only after a polled batch is projected successfully.
 * - [skipExistingSnapshots] skips snapshots already present in the target repository.
 * - [closeConsumerOnClose] controls whether [KafkaCdoSnapshotProjector.close] closes the consumer.
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
         * Creates validated options for [KafkaCdoSnapshotProjector].
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
