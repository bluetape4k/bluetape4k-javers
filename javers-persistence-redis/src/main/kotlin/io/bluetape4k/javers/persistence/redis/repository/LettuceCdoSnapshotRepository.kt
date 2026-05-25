package io.bluetape4k.javers.persistence.redis.repository

import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.bluetape4k.support.asByteArray
import io.bluetape4k.support.asInt
import io.bluetape4k.support.asLongOrNull
import io.lettuce.core.RedisClient
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Lettuce-based Redis [CdoSnapshot] repository.
 *
 * ## Behavior / Contract
 * - Snapshots are stored newest-first in a Redis LIST keyed as `javers:{name}:snapshot:{globalId}`.
 * - [saveSnapshot] uses a MULTI/EXEC transaction to atomically LPUSH the snapshot and HSET the GlobalId index.
 * - The entire MULTI/EXEC sequence is serialized with a [ReentrantLock] because the shared synchronous
 *   Lettuce connection is not thread-safe for pipelined transactions — concurrent callers would otherwise
 *   interleave commands between `multi()` and `exec()`.
 * - On transaction failure, DISCARD is attempted (failures during DISCARD are logged separately),
 *   and the original exception is propagated so that [persist] does not advance the audit-log head.
 * - The default codec is [JaversCodecs.LZ4Fory] (LZ4 + Fory serialization).
 *
 * ```kotlin
 * val repo = LettuceCdoSnapshotRepository("user", redisClient)
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(repo)
 *     .build()
 * javers.commit("author", entity)
 * val snapshots = javers.findSnapshots(queryByClass<Person>())
 * ```
 *
 * @param name repository name used as a Redis key prefix
 * @param client Lettuce [RedisClient] instance
 * @param codec the [JaversCodec] used to encode/decode [CdoSnapshot]
 */
class LettuceCdoSnapshotRepository(
    val name: String,
    private val client: RedisClient,
    codec: JaversCodec<ByteArray> = JaversCodecs.LZ4Fory,
): AbstractCdoSnapshotRepository<ByteArray>(codec) {

    companion object: KLogging() {
        private const val CACHE_KEY_SET = "globalId:set"
        private const val SEQUENCE_SET = "sequence:set"
        private const val SNAPSHOT_SUFFIX = "snapshot:"
    }

    // Redis HASH key storing [GlobalId.value()] entries for fast key lookup
    private val cacheSetKey: String = "javers:$name:$CACHE_KEY_SET"

    // Redis HASH key storing CommitId → sequence number mappings
    private val sequenceSetKey: String = "javers:$name:$SEQUENCE_SET"

    // Prefix for Redis LIST keys that store snapshots per GlobalId
    private val snapshotPrefix = "javers:$name:$SNAPSHOT_SUFFIX"

    // Read-only connection shared across all read operations.
    private val commands by lazy {
        LettuceClients.commands(client, codec = LettuceBinaryCodecs.lz4Fory())
    }

    // Dedicated connection used exclusively for MULTI/EXEC in saveSnapshot.
    // Keeping it separate from `commands` prevents read-path commands (lrange, hget, etc.)
    // from being queued into an open transaction on the same Lettuce connection.
    // `transactionLock` serializes concurrent saveSnapshot calls on this connection.
    private val transactionLock = ReentrantLock()
    private val writeCommands by lazy {
        LettuceClients.commands(client, codec = LettuceBinaryCodecs.lz4Fory())
    }

    override fun getKeys(): Set<String> {
        return commands.hkeys(cacheSetKey).sorted().toSet()
            .apply {
                log.trace { "load keys. size=${size}" }
            }
    }

    override fun contains(globalIdValue: String): Boolean {
        return commands.hexists(cacheSetKey, globalIdValue) ?: false
    }

    override fun getSeq(commitId: CommitId): Long {
        val seq = commands.hget(sequenceSetKey, commitId.value())?.asLongOrNull() ?: 0L
        log.trace { "get seq. commitId=${commitId.value()}, seq=$seq" }
        return seq
    }

    override fun updateCommitId(commitId: CommitId, sequence: Long) {
        commands.hset(sequenceSetKey, commitId.value(), sequence.toString())
    }

    override fun loadHeadId(): CommitId? {
        val latestCommitId = commands.hgetall(sequenceSetKey)
            .asSequence()
            .mapNotNull { (commitId, sequence) ->
                sequence.asLongOrNull()?.let { commitId to it }
            }
            .maxByOrNull { (_, sequence) -> sequence }
            ?.first

        return latestCommitId?.let(CommitId::valueOf)
            .also { log.trace { "Loaded head commitId=$it" } }
    }

    override fun getSnapshotSize(globalIdValue: String): Int {
        val snapshotSize = commands.llen(makeSnapshotKey(globalIdValue)).asInt()
        log.trace { "Get snapshot size=${snapshotSize}, globalId=$globalIdValue" }
        return snapshotSize
    }

    override fun saveSnapshot(snapshot: CdoSnapshot) {
        val key = makeSnapshotKey(snapshot.globalId.value())
        val value = encode(snapshot)
        transactionLock.withLock {
            try {
                writeCommands.multi()
                writeCommands.lpush(key, value)
                writeCommands.hset(cacheSetKey, snapshot.globalId.value(), snapshot.version)
                writeCommands.exec()
                log.debug { "Saved snapshot key=$key, version=${snapshot.version}" }
            } catch (e: Exception) {
                runCatching { writeCommands.discard() }
                    .onFailure { discardEx -> log.error(discardEx) { "discard() also failed" } }
                throw RuntimeException("Failed to save snapshot. globalId=${snapshot.globalId.value()}", e)
            }
        }
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> {
        val snapshots = commands
            .lrange(makeSnapshotKey(globalIdValue), 0, -1)
            .mapNotNull { decode(it.asByteArray()) }
        log.trace { "Load snapshots. globalId=$globalIdValue, size=${snapshots.size}" }
        return snapshots
    }

    /**
     * Builds the Redis LIST key for the given GlobalId value.
     *
     * @param id the [CdoSnapshot] GlobalId value (e.g. `User/1`)
     * @return the Redis key (e.g. `javers:user:snapshot:User/1`)
     */
    private fun makeSnapshotKey(id: String): String {
        return snapshotPrefix + id
    }
}
