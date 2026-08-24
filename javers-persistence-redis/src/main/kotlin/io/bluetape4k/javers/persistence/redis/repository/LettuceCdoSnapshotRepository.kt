package io.bluetape4k.javers.persistence.redis.repository

import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.bluetape4k.support.asByteArray
import io.bluetape4k.support.asInt
import io.bluetape4k.support.asLongOrNull
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import org.javers.common.exception.JaversException
import org.javers.core.commit.Commit
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Lettuce 기반 Redis [CdoSnapshot] repository입니다.
 *
 * ## 동작 / 계약
 * - Snapshot은 `javers:{name}:snapshot:{globalId}` key의 Redis LIST에 newest-first로 저장됩니다.
 * - [saveSnapshot]은 MULTI/EXEC transaction으로 snapshot LPUSH와 GlobalId index HSET을 atomic하게 수행합니다.
 * - [persist]는 JaVers commit의 모든 snapshot과 commit sequence update를 하나의 MULTI/EXEC boundary로 batch 처리합니다.
 * - [projectSnapshot]은 replay된 snapshot과 그 commit sequence를 하나의 MULTI/EXEC boundary에서 복원합니다.
 * - shared synchronous Lettuce connection은 pipelined transaction에 대해 thread-safe하지 않으므로
 *   전체 MULTI/EXEC sequence를 [ReentrantLock]으로 직렬화합니다. 그렇지 않으면 concurrent caller가
 *   `multi()`와 `exec()` 사이에 command를 interleave할 수 있습니다.
 * - transaction failure 시 DISCARD를 시도합니다(DISCARD 중 failure는 별도로 log합니다).
 *   원래 exception은 전파되어 [persist]가 audit-log head를 advance하지 않습니다.
 * - 기본 codec은 [JaversCodecs.LZ4Fory](LZ4 + Fory serialization)입니다.
 * - caller가 [client]를 소유합니다. 이 repository는 해당 client에서 연 read/write connection만 소유하며
 *   [close]에서 닫습니다.
 * - [close]는 terminal lifecycle입니다. close 이후 모든 read/write operation은
 *   `IllegalStateException`으로 거부하며 connection을 다시 열지 않습니다.
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
 * @param name Redis key prefix로 사용하는 repository name입니다.
 * @param client Lettuce [RedisClient] instance입니다.
 * @param codec [CdoSnapshot] encode/decode에 사용하는 [JaversCodec]입니다.
 */
class LettuceCdoSnapshotRepository(
    val name: String,
    private val client: RedisClient,
    codec: JaversCodec<ByteArray> = JaversCodecs.LZ4Fory,
): AbstractCdoSnapshotRepository<ByteArray>(codec), AutoCloseable {

    companion object: KLogging() {
        private const val CACHE_KEY_SET = "globalId:set"
        private const val SEQUENCE_SET = "sequence:set"
        private const val SNAPSHOT_SUFFIX = "snapshot:"
    }

    // 빠른 key lookup을 위해 [GlobalId.value()] entry를 저장하는 Redis HASH key입니다.
    private val cacheSetKey: String = "javers:$name:$CACHE_KEY_SET"

    // CommitId -> sequence number mapping을 저장하는 Redis HASH key입니다.
    private val sequenceSetKey: String = "javers:$name:$SEQUENCE_SET"

    // GlobalId별 snapshot을 저장하는 Redis LIST key prefix입니다.
    private val snapshotPrefix = "javers:$name:$SNAPSHOT_SUFFIX"

    private val redisCodec = LettuceBinaryCodecs.lz4Fory<Any>()

    // 이 repository가 소유하는 read-only connection입니다.
    private val readConnection = lazy { client.connect(redisCodec) }
    private val readCommands by lazy { readConnection.value.sync() }
    private val commands get() = openResource { readCommands }

    // saveSnapshot의 MULTI/EXEC에만 사용하는 전용 connection입니다.
    // `commands`와 분리해 read-path command(lrange, hget 등)가 같은 Lettuce connection의
    // open transaction에 queue되는 것을 방지합니다.
    // `transactionLock`은 이 connection의 concurrent saveSnapshot 호출을 직렬화합니다.
    private val transactionLock = ReentrantLock()
    private val closeLock = ReentrantLock()
    @Volatile
    private var closed = false
    private val writeConnection = lazy { client.connect(redisCodec) }
    private val writeCommandDelegate by lazy { writeConnection.value.sync() }
    private val writeCommands get() = openResource { writeCommandDelegate }

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
        log.trace { "get seq. ${RedisIdentifierDiagnostics.format(commitId.value(), "commitId")}, seq=$seq" }
        return seq
    }

    override fun updateCommitId(commitId: CommitId, sequence: Long) {
        commands.hset(sequenceSetKey, commitId.value(), sequence.toString())
    }

    override fun loadHeadId(): CommitId? {
        val latestCommitId = commands.hgetall(sequenceSetKey)
            .asSequence()
            .map { (commitIdValue, sequenceValue) ->
                val sequenceText = sequenceValue.toString()
                val sequence = sequenceValue.asLongOrNull()
                    ?: corruptedMetadata("sequence", sequenceText)
                val commitId = parseCommitId(commitIdValue)
                    ?: corruptedMetadata("commitId", commitIdValue)
                commitId to sequence
            }
            .maxByOrNull { (_, sequence) -> sequence }
            ?.first

        return latestCommitId
            .also { commitId ->
                log.trace {
                    "Loaded head metadata. " +
                        (commitId?.let { RedisIdentifierDiagnostics.format(it.value(), "commitId") }
                            ?: "present=false")
                }
            }
    }

    override fun getSnapshotSize(globalIdValue: String): Int {
        val snapshotSize = commands.llen(makeSnapshotKey(globalIdValue)).asInt()
        log.trace {
            "Get snapshot size=$snapshotSize, ${RedisIdentifierDiagnostics.format(globalIdValue, "globalId")}"
        }
        return snapshotSize
    }

    override fun saveSnapshot(snapshot: CdoSnapshot) {
        ensureOpen()
        val value = encode(snapshot)
        transactionLock.withLock {
            try {
                writeCommands.multi()
                queueSaveSnapshot(snapshot, value)
                writeCommands.exec()
                log.debug {
                    "Saved snapshot. " +
                        RedisIdentifierDiagnostics.format(snapshot.globalId.value(), "globalId") +
                        ", version=${snapshot.version}"
                }
            } catch (e: Exception) {
                runCatching { writeCommands.discard() }
                    .onFailure { discardEx -> log.error(discardEx) { "discard() also failed" } }
                throw RuntimeException(
                    "Failed to save snapshot. " +
                        RedisIdentifierDiagnostics.format(snapshot.globalId.value(), "globalId"),
                    e,
                )
            }
        }
    }

    override fun persistCommit(commit: Commit, sequence: Long) {
        ensureOpen()
        val encodedSnapshots = commit.snapshots.map { snapshot -> snapshot to encode(snapshot) }
        transactionLock.withLock {
            try {
                writeCommands.multi()
                encodedSnapshots.forEach { (snapshot, value) ->
                    queueSaveSnapshot(snapshot, value)
                }
                writeCommands.hset(sequenceSetKey, commit.id.value(), sequence.toString())
                writeCommands.exec()
                log.debug {
                    "Persisted Redis snapshot commit. " +
                        RedisIdentifierDiagnostics.format(commit.id.value(), "commitId") +
                        ", snapshots=${encodedSnapshots.size}"
                }
            } catch (e: Exception) {
                runCatching { writeCommands.discard() }
                    .onFailure { discardEx -> log.error(discardEx) { "discard() also failed" } }
                throw RuntimeException(
                    "Failed to persist snapshot commit. " +
                        RedisIdentifierDiagnostics.format(commit.id.value(), "commitId"),
                    e,
                )
            }
        }
    }

    override fun persistProjectedSnapshot(snapshot: CdoSnapshot, sequence: Long) {
        ensureOpen()
        val value = encode(snapshot)
        transactionLock.withLock {
            try {
                writeCommands.multi()
                queueSaveSnapshot(snapshot, value)
                writeCommands.hset(sequenceSetKey, snapshot.commitMetadata.id.value(), sequence.toString())
                writeCommands.exec()
                log.debug {
                    "Projected Redis snapshot. " +
                        RedisIdentifierDiagnostics.format(snapshot.commitMetadata.id.value(), "commitId") +
                        ", version=${snapshot.version}"
                }
            } catch (e: Exception) {
                runCatching { writeCommands.discard() }
                    .onFailure { discardEx -> log.error(discardEx) { "discard() also failed" } }
                throw RuntimeException(
                    "Failed to project snapshot. " +
                        RedisIdentifierDiagnostics.format(snapshot.globalId.value(), "globalId"),
                    e,
                )
            }
        }
    }

    private fun queueSaveSnapshot(snapshot: CdoSnapshot, value: ByteArray) {
        val key = makeSnapshotKey(snapshot.globalId.value())
        writeCommands.lpush(key, value)
        writeCommands.hset(cacheSetKey, snapshot.globalId.value(), snapshot.version)
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> {
        val snapshots = commands
            .lrange(makeSnapshotKey(globalIdValue), 0, -1)
            .mapNotNull { decode(it.asByteArray()) }
        log.trace {
            "Load snapshots. " +
                RedisIdentifierDiagnostics.format(globalIdValue, "globalId") +
                ", size=${snapshots.size}"
        }
        return snapshots
    }

    /**
     * 지정한 GlobalId 값에 대한 Redis LIST key를 만듭니다.
     *
     * @param id [CdoSnapshot] GlobalId 값입니다(예: `User/1`).
     * @return Redis key입니다(예: `javers:user:snapshot:User/1`).
     */
    private fun makeSnapshotKey(id: String): String {
        return snapshotPrefix + id
    }

    override fun close() {
        closeLock.withLock {
            if (closed) {
                return
            }

            closed = true
            closeConnection("write", writeConnection)
            closeConnection("read", readConnection)
        }
    }

    private inline fun <T> openResource(resource: () -> T): T {
        ensureOpen()
        return resource()
    }

    private fun ensureOpen() {
        check(!closed) {
            "Lettuce repository is already closed. " +
                RedisIdentifierDiagnostics.format(name, "repository")
        }
    }

    private fun parseCommitId(value: String): CommitId? = try {
        CommitId.valueOf(value)
    } catch (_: JaversException) {
        null
    } catch (_: NumberFormatException) {
        null
    }

    private fun corruptedMetadata(type: String, value: String): Nothing {
        error("Corrupted Redis head metadata. ${RedisIdentifierDiagnostics.format(value, type)}")
    }

    private fun closeConnection(
        role: String,
        connection: Lazy<StatefulRedisConnection<String, Any>>,
    ) {
        if (!connection.isInitialized()) {
            return
        }

        runCatching { connection.value.close() }
            .onFailure { e ->
                log.warn(e) {
                    "Failed to close $role Lettuce connection. " +
                        RedisIdentifierDiagnostics.format(name, "repository")
                }
            }
    }
}
