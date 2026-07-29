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
 * Caffeine cache 기반 in-memory [CdoSnapshot] repository입니다.
 *
 * ## 동작 / 계약
 * - [Cache] instance를 사용해 GlobalId별 snapshot list를 memory에 보관합니다.
 * - 최신 snapshot이 먼저 오도록 list 앞쪽(index 0)에 snapshot을 삽입합니다.
 * - 동시 접근 중 [java.util.ConcurrentModificationException]이 발생하지 않도록
 *   [saveSnapshot]과 [loadSnapshots]를 lock으로 보호합니다.
 *
 * ```kotlin
 * val repo = CaffeineCdoSnapshotRepository()
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(repo)
 *     .build()
 * ```
 *
 * @param codec snapshot encode/decode에 사용하는 [JaversCodec]입니다. 기본값은 LZ4로 압축한 string codec입니다.
 */
class CaffeineCdoSnapshotRepository(
    codec: JaversCodec<String> = JaversCodecs.LZ4String,
): AbstractCdoSnapshotRepository<String>(codec) {

    companion object: KLogging()

    private val lock = ReentrantLock()

    /**
     * GlobalId별 encode된 snapshot list를 보관하는 cache입니다(key=globalId, value=encode된 snapshot list).
     */
    private val snapshotCache: Cache<String, MutableList<String>> by lazy {
        caffeine {
            initialCapacity(1_000)
        }.build()
    }

    /**
     * 각 [CommitId]의 sequence number를 보관하는 cache입니다.
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
