package io.bluetape4k.javers.repository.jcache

import io.bluetape4k.cache.jcache.getOrCreate
import io.bluetape4k.cache.jcache.jcacheConfiguration
import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import java.util.concurrent.locks.ReentrantLock
import javax.cache.expiry.EternalExpiryPolicy
import kotlin.concurrent.withLock

/**
 * JCache(JSR-107) 기반 [CdoSnapshot] repository입니다.
 *
 * ## 계약
 * - [javax.cache.Cache]를 사용해 GlobalId별로 묶은 snapshot을 저장합니다.
 * - JCache는 값을 copy로 저장하므로, write 시 갱신된 list를 다시 cache에 넣어야 합니다.
 * - 동시 write가 안전하도록 lock을 사용합니다.
 *
 * @param prefix 생성할 JCache cache 이름 앞에 붙이는 prefix입니다. Snapshot cache와 commit sequence cache 이름을 분리하는 데 사용합니다.
 * @param cacheManager snapshot 및 commit sequence cache를 생성하거나 조회할 [javax.cache.CacheManager] instance입니다.
 * @param codec snapshot encode/decode에 사용하는 [JaversCodec]입니다. 기본값은 LZ4로 압축한 string codec입니다.
 */
class JCacheCdoSnapshotRepository(
    prefix: String,
    cacheManager: javax.cache.CacheManager,
    codec: JaversCodec<String> = JaversCodecs.LZ4String,
): AbstractCdoSnapshotRepository<String>(codec) {

    companion object: KLogging() {
        private const val SNAPSHOT_SUFFIX = "-snapshots"
        private const val COMMIT_SEQ_SUFFIX = "-commit_seq"
    }

    private val lock = ReentrantLock()

    private val snapshotCacheName = prefix + SNAPSHOT_SUFFIX
    private val commitSeqCacheName = prefix + COMMIT_SEQ_SUFFIX

    private val snapshotCache: javax.cache.Cache<String, MutableList<String>> by lazy {
        val cfg = jcacheConfiguration<String, MutableList<String>> {
            setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf())
            setStoreByValue(true)
        }
        cacheManager.getOrCreate(snapshotCacheName, cfg)
    }
    private val commitSeqCache: javax.cache.Cache<CommitId, Long> by lazy {
        val cfg = jcacheConfiguration<CommitId, Long> {
            setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf())
            setStoreByValue(true)
        }
        cacheManager.getOrCreate(commitSeqCacheName, cfg)
    }

    override fun getKeys(): Set<String> {
        return snapshotCache.map { it.key }.toSet()
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

            // NOTE: JCache는 보통 reference로 저장하지만, 이 repository는 store-by-value semantics가 필요합니다.
            val snapshots = snapshotCache.get(globalIdValue) ?: mutableListOf()
            val encoded = encode(snapshot)
            snapshots.add(0, encoded)
            snapshotCache.put(globalIdValue, snapshots)
        }
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> {
        return snapshotCache[globalIdValue]?.mapNotNull { decode(it) } ?: emptyList()
    }
}
