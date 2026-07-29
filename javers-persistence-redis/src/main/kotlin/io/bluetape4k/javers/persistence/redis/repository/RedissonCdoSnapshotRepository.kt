package io.bluetape4k.javers.persistence.redis.repository

import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.redisson.api.RListMultimap
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import org.redisson.client.codec.LongCodec
import org.redisson.client.codec.StringCodec
import org.redisson.codec.CompositeCodec

/**
 * Redisson 기반 Redis [CdoSnapshot] repository입니다.
 *
 * ## 동작 / 계약
 * - Snapshot byte array는 GlobalId별로 [RListMultimap]에 저장됩니다.
 * - [loadSnapshots]는 multimap에서 entry를 조회하고 역순(most-recent first)으로 반환합니다.
 * - CommitId -> sequence number mapping은 string key와 long value를 사용하는 [RMap]에 저장됩니다.
 * - commit 및 projection metadata persistence는 Redisson data structure 전체에서 best-effort입니다.
 *   snapshot write 하나 이후 failure가 발생하면 restored head sequence를 advance하지 않은 partial snapshot data가 남을 수 있습니다.
 *   Redis commit-level 또는 projection-level MULTI/EXEC atomicity가 필요하면 Lettuce repository를 사용하세요.
 * - 기본 codec은 [JaversCodecs.LZ4Fory](LZ4 compression + Fory serialization)입니다.
 *
 * ```kotlin
 * val repo = RedissonCdoSnapshotRepository("user", redissonClient)
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(repo)
 *     .build()
 * javers.commit("author", entity)
 * val snapshots = javers.findSnapshots(queryByClass<Person>())
 * ```
 *
 * @param name Redis key prefix로 사용하는 repository name입니다.
 * @param redisson [RedissonClient] instance입니다.
 * @param codec [CdoSnapshot] instance encode/decode에 사용하는 [JaversCodec]입니다.
 */
class RedissonCdoSnapshotRepository(
    val name: String,
    private val redisson: RedissonClient,
    codec: JaversCodec<ByteArray> = JaversCodecs.LZ4Fory,
): AbstractCdoSnapshotRepository<ByteArray>(codec) {

    companion object: KLogging() {
        private const val SEQUENCE = "sequence"
        private const val SNAPSHOT = "snapshot"
    }

    private val sequenceName: String = "javers:$name:$SEQUENCE"
    private val snapshotName: String = "javers:$name:$SNAPSHOT"

    /**
     * GlobalId별 snapshot byte array를 저장하는 multimap입니다.
     */
    private val snapshots: RListMultimap<String, ByteArray> =
        redisson.getListMultimap(snapshotName, RedissonCodecs.LZ4ForyComposite)

    /**
     * CommitId와 sequence number mapping을 저장하는 map입니다.
     */
    private val commitIdSequences: RMap<String, Long> =
        redisson.getMap(sequenceName, CompositeCodec(StringCodec.INSTANCE, LongCodec.INSTANCE))

    override fun getKeys(): Set<String> {
        return snapshots.keySet().sorted().toSet()
            .apply {
                log.trace { "load keys. size=$size" }
            }
    }

    override fun contains(globalIdValue: String): Boolean {
        return snapshots.containsKey(globalIdValue)
    }

    override fun getSeq(commitId: CommitId): Long {
        val seq = commitIdSequences.getOrDefault(commitId.value(), 0L)
        log.trace { "get seq. commitId=${commitId.value()}, seq=$seq" }
        return seq
    }

    override fun updateCommitId(commitId: CommitId, sequence: Long) {
        commitIdSequences.fastPut(commitId.value(), sequence)
    }

    override fun loadHeadId(): CommitId? {
        val latestCommitId = commitIdSequences.readAllEntrySet()
            .asSequence()
            .mapNotNull { entry ->
                val commitId = runCatching { CommitId.valueOf(entry.key) }.getOrNull()
                    ?: return@mapNotNull null
                commitId to entry.value
            }
            .maxByOrNull { (_, sequence) -> sequence }
            ?.first

        return latestCommitId
            .also { log.trace { "Loaded head commitId=$it" } }
    }

    override fun getSnapshotSize(globalIdValue: String): Int {
        return snapshots[globalIdValue].size
    }

    override fun saveSnapshot(snapshot: CdoSnapshot) {
        val key = snapshot.globalId.value()
        val value = encode(snapshot)
        val saved = snapshots.put(key, value)
        log.trace { "Save snapshot [$saved]. key=[$key], version=[${snapshot.version}]" }
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> {
        // NOTE: most-recent snapshot이 먼저 오도록 entry를 reverse해야 합니다(stack-like access).
        val loaded = snapshots.getAll(globalIdValue)
            .mapNotNull { value ->
                log.debug { "value size=${value.size}" }
                if (value.isNotEmpty()) decode(value) else null
            }
            .reversed()
        log.trace { "Load snapshots. globalId=$globalIdValue, size=${loaded.size}" }
        return loaded
    }
}
