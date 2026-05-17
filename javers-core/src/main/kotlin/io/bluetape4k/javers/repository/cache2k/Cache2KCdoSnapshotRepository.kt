package io.bluetape4k.javers.repository.cache2k

import io.bluetape4k.cache.cache2k.cache2k
import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import org.cache2k.Cache
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Cache2k-backed in-memory [CdoSnapshot] repository.
 *
 * ## Behavior / Contract
 * - Uses a [Cache] instance to hold snapshot lists per GlobalId in memory.
 * - Snapshots are inserted at the front of the list (index 0) so the newest entry comes first.
 * - Both [saveSnapshot] and [loadSnapshots] are protected by a lock to prevent
 *   [java.util.ConcurrentModificationException] during concurrent access.
 *
 * ```kotlin
 * val repo = Cache2KCdoSnapshotRepository()
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(repo)
 *     .build()
 * ```
 *
 * @param codec the [JaversCodec] used to encode/decode snapshots (default: LZ4-compressed string)
 */
class Cache2KCdoSnapshotRepository(
    codec: JaversCodec<String> = JaversCodecs.LZ4String,
): AbstractCdoSnapshotRepository<String>(codec) {

    companion object: KLogging()

    private val lock = ReentrantLock()

    private val snapshotCache: Cache<String, MutableList<String>> by lazy {
        cache2k<String, MutableList<String>> {
            this.entryCapacity(100_000)
            this.storeByReference(true)
            this.eternal(true)
        }.build()
    }

    private val commitSeqCache: Cache<CommitId, Long> by lazy {
        cache2k<CommitId, Long> {
            this.entryCapacity(100_000)
            this.eternal(true)
        }.build()
    }

    override fun getKeys(): Set<String> {
        return snapshotCache.keys()
    }

    override fun contains(globalIdValue: String): Boolean {
        return snapshotCache.containsKey(globalIdValue)
    }

    override fun getSeq(commitId: CommitId): Long = commitSeqCache[commitId] ?: 0L

    override fun updateCommitId(commitId: CommitId, sequence: Long) {
        commitSeqCache.put(commitId, sequence)
    }

    override fun getSnapshotSize(globalIdValue: String): Int {
        return snapshotCache[globalIdValue]?.size ?: 0
    }

    override fun saveSnapshot(snapshot: CdoSnapshot) {
        lock.withLock {
            val globalIdValue = snapshot.globalId.value()
            val snapshots = snapshotCache.computeIfAbsent(globalIdValue) { mutableListOf() }
            val encoded = encode(snapshot)
            snapshots.add(0, encoded)
        }
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> {
        val encoded = lock.withLock {
            snapshotCache[globalIdValue]?.toList() ?: emptyList()
        }
        return encoded.mapNotNull { decode(it) }
    }
}
