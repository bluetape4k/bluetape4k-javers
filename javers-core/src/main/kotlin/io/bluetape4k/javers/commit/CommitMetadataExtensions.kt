package io.bluetape4k.javers.commit

import org.javers.core.commit.CommitId
import org.javers.core.commit.CommitMetadata

/**
 * 이 [CommitId]의 major/minor id를 [Pair]로 반환합니다.
 *
 * ```kotlin
 * val (major, minor) = commitId.version
 * // major == commitId.majorId, minor == commitId.minorId
 * ```
 */
val CommitId.version: Pair<Long, Int> get() = Pair(majorId, minorId)

/**
 * 두 [CommitMetadata] 값을 commit id 기준으로 비교합니다.
 */
operator fun CommitMetadata.compareTo(that: CommitMetadata): Int =
    this.id.compareTo(that.id)

/**
 * commit 시간을 epoch milliseconds로 반환합니다.
 *
 * ```kotlin
 * val ts = commitMetadata.commitTimestamp
 * // ts > 0
 * ```
 */
val CommitMetadata.commitTimestamp: Long get() = commitDateInstant.toEpochMilli()
