package io.bluetape4k.javers.repository.caffeine

import com.github.benmanes.caffeine.cache.Cache
import io.bluetape4k.cache.caffeine.caffeine
import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Caffeine-cache-backed in-memory [CdoSnapshot] repository.
 *
 * ## Behavior / Contract
 * - Uses a [Cache] instance to hold snapshot lists per GlobalId in memory.
 * - Snapshots are inserted at the front of the list (index 0) so the newest entry comes first.
 * - Both [saveSnapshot] and [loadSnapshots] are protected by a lock to prevent
 *   [java.util.ConcurrentModificationException] during concurrent access.
 *
 * ```kotlin
 * val repo = CaffeineCdoSnapshotRepository()
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(repo)
 *     .build()
 * ```
 *
 * @param codec the [JaversCodec] used to encode/decode snapshots (default: LZ4-compressed string)
 */
class CaffeineCdoSnapshotRepository(
    codec: JaversCodec<String> = JaversCodecs.LZ4String,
): AbstractCdoSnapshotRepository<String>(codec) {

    companion object: KLogging()

    private val lock = ReentrantLock()

    /**
     * Cache holding encoded snapshot lists per GlobalId (key=globalId, value=list of encoded snapshots).
     */
    private val snapshotCache: Cache<String, MutableList<String>> by lazy {
        caffeine {
            initialCapacity(1_000)
        }.build()
    }

    /**
     * Cache holding the sequence number for each [CommitId].
     */
    private val commitSeqCache: Cache<CommitId, Long> by lazy {
        caffeine {
            initialCapacity(1_000)
        }.build()
    }

    override fun getKeys(): Set<String> {
        return snapshotCache.asMap().map { it.key }.toSet()
    }

    override fun contains(globalIdValue: String): Boolean {
        return snapshotCache.getIfPresent(globalIdValue) != null
    }

    override fun getSeq(commitId: CommitId): Long = commitSeqCache.getIfPresent(commitId) ?: 0L

    override fun updateCommitId(commitId: CommitId, sequence: Long) {
        commitSeqCache.put(commitId, sequence)
    }

    override fun getSnapshotSize(globalIdValue: String): Int {
        return snapshotCache.getIfPresent(globalIdValue)?.size ?: 0
    }

    override fun saveSnapshot(snapshot: CdoSnapshot) {
        lock.withLock {
            val globalIdValue = snapshot.globalId.value()
            val snapshots = snapshotCache.get(globalIdValue) { _ -> mutableListOf() }
            val encoded = encode(snapshot)
            snapshots.add(0, encoded)
        }
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> {
        val encoded = lock.withLock {
            snapshotCache.getIfPresent(globalIdValue)?.toList() ?: emptyList()
        }
        return encoded.mapNotNull { decode(it) }
    }
}
