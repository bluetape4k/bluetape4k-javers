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
 * Redisson-backed Redis [CdoSnapshot] repository.
 *
 * ## Behavior / Contract
 * - Snapshot byte arrays are stored per GlobalId in an [RListMultimap].
 * - [loadSnapshots] retrieves entries from the multimap and returns them in reverse order (most-recent first).
 * - CommitId → sequence number mappings are stored in an [RMap] using string keys and long values.
 * - Commit persistence is best-effort across Redisson data structures. A failure after one snapshot write
 *   can leave partial snapshot data without advancing the restored head sequence. Use the Lettuce repository
 *   when Redis commit-level MULTI/EXEC atomicity is required.
 * - The default codec is [JaversCodecs.LZ4Fory] (LZ4 compression + Fory serialization).
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
 * @param name Repository name used as the Redis key prefix
 * @param redisson [RedissonClient] instance
 * @param codec [JaversCodec] used to encode and decode [CdoSnapshot] instances
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
     * Multimap storing snapshot byte arrays per GlobalId.
     */
    private val snapshots: RListMultimap<String, ByteArray> =
        redisson.getListMultimap(snapshotName, RedissonCodecs.LZ4ForyComposite)

    /**
     * Map storing CommitId to sequence number mappings.
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
        // NOTE: Entries must be reversed so the most-recent snapshot comes first (stack-like access).
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
