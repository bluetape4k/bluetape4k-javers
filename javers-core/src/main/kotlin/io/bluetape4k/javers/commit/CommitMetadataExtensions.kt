package io.bluetape4k.javers.commit

import org.javers.core.commit.CommitId
import org.javers.core.commit.CommitMetadata

/**
 * Returns the major and minor ids of this [CommitId] as a [Pair].
 *
 * ```kotlin
 * val (major, minor) = commitId.version
 * // major == commitId.majorId, minor == commitId.minorId
 * ```
 */
val CommitId.version: Pair<Long, Int> get() = Pair(majorId, minorId)

/**
 * Compares two [CommitMetadata] values by commit id.
 */
operator fun CommitMetadata.compareTo(that: CommitMetadata): Int =
    this.id.compareTo(that.id)

/**
 * Returns the commit time as epoch milliseconds.
 *
 * ```kotlin
 * val ts = commitMetadata.commitTimestamp
 * // ts > 0
 * ```
 */
val CommitMetadata.commitTimestamp: Long get() = commitDateInstant.toEpochMilli()
