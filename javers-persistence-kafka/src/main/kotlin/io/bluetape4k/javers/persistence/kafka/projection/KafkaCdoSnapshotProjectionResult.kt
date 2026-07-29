package io.bluetape4k.javers.persistence.kafka.projection

import io.bluetape4k.support.requireGe
import java.io.Serializable

/**
 * [KafkaCdoSnapshotProjector]가 반환하는 result counter입니다.
 */
@ConsistentCopyVisibility
data class KafkaCdoSnapshotProjectionResult private constructor(
    val polledRecords: Int,
    val projectedSnapshots: Int,
    val skippedSnapshots: Int,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 7452692371867985878L

        /**
         * validation된 projection counter를 생성합니다.
         */
        operator fun invoke(
            polledRecords: Int = 0,
            projectedSnapshots: Int = 0,
            skippedSnapshots: Int = 0,
        ): KafkaCdoSnapshotProjectionResult {
            polledRecords.requireGe(0, "polledRecords")
            projectedSnapshots.requireGe(0, "projectedSnapshots")
            skippedSnapshots.requireGe(0, "skippedSnapshots")

            return KafkaCdoSnapshotProjectionResult(
                polledRecords = polledRecords,
                projectedSnapshots = projectedSnapshots,
                skippedSnapshots = skippedSnapshots,
            )
        }
    }

    operator fun plus(other: KafkaCdoSnapshotProjectionResult): KafkaCdoSnapshotProjectionResult =
        KafkaCdoSnapshotProjectionResult(
            polledRecords = polledRecords + other.polledRecords,
            projectedSnapshots = projectedSnapshots + other.projectedSnapshots,
            skippedSnapshots = skippedSnapshots + other.skippedSnapshots,
        )
}
