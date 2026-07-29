package io.bluetape4k.javers.repository

import com.google.gson.JsonObject
import io.bluetape4k.idgenerators.snowflake.Snowflake
import io.bluetape4k.idgenerators.snowflake.Snowflakers
import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.metamodel.filterByAuthor
import io.bluetape4k.javers.metamodel.filterByChangedPropertyNames
import io.bluetape4k.javers.metamodel.filterByCommitDate
import io.bluetape4k.javers.metamodel.filterByCommitIds
import io.bluetape4k.javers.metamodel.filterByCommitProperties
import io.bluetape4k.javers.metamodel.filterByToCommitId
import io.bluetape4k.javers.metamodel.filterByType
import io.bluetape4k.javers.metamodel.filterByVersion
import io.bluetape4k.javers.metamodel.isChild
import io.bluetape4k.javers.metamodel.isParent
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import io.bluetape4k.support.toOptional
import org.javers.core.commit.Commit
import org.javers.core.commit.CommitId
import org.javers.core.json.JsonConverter
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.core.metamodel.`object`.GlobalId
import org.javers.core.metamodel.`object`.ValueObjectId
import org.javers.core.metamodel.type.EntityType
import org.javers.core.metamodel.type.ManagedType
import org.javers.repository.api.QueryParams
import org.javers.repository.api.SnapshotIdentifier
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.jvm.optionals.getOrNull

/**
 * [CdoSnapshotRepository]의 공통 구현을 제공하는 abstract class입니다.
 *
 * ## 동작 / 계약
 * - [JaversCodec]을 사용해 [CdoSnapshot]과 encode된 [T] 형식 사이를 변환합니다.
 * - [persist]는 lock으로 thread-safe하게 실행되며, [Snowflake] 기반 sequence number를 배정한 뒤
 *   전체 commit write를 [persistCommit]에 위임합니다.
 * - [projectSnapshot]은 decode된 replay snapshot과 함께 commit head 및 sequence metadata를 복원합니다.
 * - author, date, version, commitId 등 공통 [QueryParams] 기반 filtering을 여기서 처리합니다.
 * - subclass는 [getKeys], [contains], [getSeq], [updateCommitId], [getSnapshotSize],
 *   [saveSnapshot], [loadSnapshots]를 구현해야 합니다.
 * - persistent subclass는 repository rebuild 후 최신 persisted head commit을 복원하기 위해
 *   [loadHeadId]를 override할 수 있습니다.
 *
 * @param T encode된 snapshot data의 type입니다(예: String, ByteArray).
 * @property codec snapshot encode/decode에 사용하는 [JaversCodec]입니다.
 * @property commitIdSupplier commit sequence number 생성에 사용하는 [Snowflake]입니다.
 */
abstract class AbstractCdoSnapshotRepository<T: Any>(
    protected val codec: JaversCodec<T>,
    protected val commitIdSupplier: Snowflake = Snowflakers.Global,
): CdoSnapshotRepository {

    companion object: KLogging()

    private var jsonConverter: JsonConverter? = null
    private val lock = ReentrantLock()

    fun getJsonConverter(): JsonConverter? = jsonConverter

    override fun setJsonConverter(jsonConverter: JsonConverter?) {
        this.jsonConverter = jsonConverter
    }

    protected abstract fun getKeys(): Set<String>
    protected abstract fun contains(globalIdValue: String): Boolean
    protected fun contains(globalId: GlobalId): Boolean = contains(globalId.value())

    protected abstract fun getSeq(commitId: CommitId): Long
    protected abstract fun updateCommitId(commitId: CommitId, sequence: Long)

    /**
     * persistent storage에서 최신 head commit을 복원합니다.
     *
     * repository가 비어 있거나 head 복원을 지원하지 않으면 `null`을 반환합니다.
     */
    protected open fun loadHeadId(): CommitId? = null

    protected abstract fun getSnapshotSize(globalIdValue: String): Int
    protected fun getSnapshotSize(globalId: GlobalId): Int = getSnapshotSize(globalId.value())

    @Volatile
    protected var head: CommitId? = null

    @Volatile
    private var headLoaded: Boolean = false

    protected fun encode(snapshot: CdoSnapshot): T {
        val converter = requireNotNull(jsonConverter) {
            "JsonConverter is not set. Ensure Javers called setJsonConverter() before encoding."
        }
        val jsonObject = converter.toJsonElement(snapshot) as JsonObject
        return doEncode(jsonObject)
    }

    protected fun decode(data: T): CdoSnapshot? {
        val jsonObject = doDecode(data)
        return jsonObject?.let { jsonConverter?.fromJson(it, CdoSnapshot::class.java) }
    }

    protected fun doEncode(jsonObject: JsonObject): T = codec.encode(jsonObject)
    protected fun doDecode(data: T): JsonObject? = codec.decode(data)

    /**
     * repository의 모든 snapshot을 sequence number 내림차순으로 정렬해 load합니다.
     *
     * ## OOM 경고
     * 이 method는 filtering 전에 모든 snapshot을 memory에 materialize합니다.
     * 대형 repository에서는 제한 없는 memory allocation이 될 수 있습니다.
     * key count가 10,000개를 넘으면 warning을 기록합니다.
     * Production에서는 JQL query 기반 접근([getSnapshots], [getStateHistory])을 우선 사용하세요.
     */
    protected fun getAll(): List<CdoSnapshot> {
        val keys = getKeys()
        if (keys.size > 10_000) {
            log.warn { "getAll() is loading ${keys.size} keys — this may cause OutOfMemoryError for large repositories. Use query-based JQL instead." }
        }
        return keys
            .flatMap { loadSnapshots(it) }
            .sortedByDescending { getSeq(it.commitMetadata.id) }
            .also {
                log.debug { "Loaded all snapshots. size=${it.size}" }
            }
    }

    override fun ensureSchema() {
        // 수행할 작업이 없습니다.
    }

    override fun getLatest(globalId: GlobalId): Optional<CdoSnapshot> = when {
        contains(globalId) -> loadSnapshots(globalId).firstOrNull().toOptional()
        else -> Optional.empty()
    }

    override fun getLatest(globalIds: MutableCollection<GlobalId>): MutableList<CdoSnapshot> {
        return globalIds.mapNotNull { getLatest(it).getOrNull() }.toMutableList()
    }

    override fun getStateHistory(globalId: GlobalId, queryParams: QueryParams): MutableList<CdoSnapshot> {
        val filtered = mutableListOf<CdoSnapshot>()
        getAll().forEach snapshot@{ snapshot ->
            if (snapshot.globalId == globalId) {
                filtered.add(snapshot)
                return@snapshot
            }
            if (queryParams.isAggregate && globalId.isParent(snapshot.globalId)) {
                filtered.add(snapshot)
                return@snapshot
            }
        }
        return applyQueryParams(filtered.asSequence(), queryParams)
    }

    override fun getStateHistory(
        givenClasses: MutableSet<ManagedType>,
        queryParams: QueryParams,
    ): MutableList<CdoSnapshot> {
        val filtered = mutableListOf<CdoSnapshot>()

        getAll().forEach snapshot@{ snapshot ->
            givenClasses.forEach classes@{ givenClass ->
                if (snapshot.globalId.isTypeOf(givenClass)) {
                    filtered.add(snapshot)
                    return@classes
                }
                if (queryParams.isAggregate && snapshot.globalId.isChild(givenClass)) {
                    filtered.add(snapshot)
                    return@classes
                }
            }
        }
        return applyQueryParams(filtered.asSequence(), queryParams)
    }

    override fun getValueObjectStateHistory(
        ownerEntity: EntityType,
        path: String,
        queryParams: QueryParams,
    ): MutableList<CdoSnapshot> {
        val result = getAll().filter { snapshot ->
            val id = snapshot.globalId
            id is ValueObjectId && id.hasOwnerOfType(ownerEntity) && id.fragment.equals(path)
        }
        return applyQueryParams(result.asSequence(), queryParams)
    }

    override fun getSnapshots(queryParams: QueryParams): MutableList<CdoSnapshot> {
        return applyQueryParams(getAll().asSequence(), queryParams)
    }

    override fun getSnapshots(snapshotIdentifiers: MutableCollection<SnapshotIdentifier>): List<CdoSnapshot> {
        log.trace { "get snapshots by identifiers. $snapshotIdentifiers" }
        return getPersistedIdentifiers(snapshotIdentifiers)
            .mapNotNull { snapshot ->
                loadSnapshots(snapshot.globalId)
                    .firstOrNull { it.version == snapshot.version }
            }
    }

    override fun persist(commit: Commit?) {
        if (commit == null) {
            return
        }
        lock.withLock {
            val sequence = commitIdSupplier.nextId()
            persistCommit(commit, sequence)
            log.trace { "${commit.snapshots.size} snapshot(s) persisted" }
            head = commit.id
            headLoaded = true
        }
    }

    /**
     * [commit]의 모든 snapshot을 persist하고 commit [sequence]를 저장합니다.
     *
     * ## 계약
     * 기본 구현은 기존 per-snapshot write 동작을 보존합니다.
     * commit-level atomicity를 제공할 수 있는 durable repository는 이 method를 override하고
     * 전체 operation을 backend의 transaction 또는 batch primitive로 감싸야 합니다.
     */
    protected open fun persistCommit(commit: Commit, sequence: Long) {
        commit.snapshots.forEach {
            saveSnapshot(it)
        }
        updateCommitId(commit.id, sequence)
    }

    override fun projectSnapshot(snapshot: CdoSnapshot) {
        lock.withLock {
            val commitId = snapshot.commitMetadata.id
            val sequence = getSeq(commitId).takeIf { it > 0L } ?: commitIdSupplier.nextId()

            persistProjectedSnapshot(snapshot, sequence)
            restoreHeadAfterProjection(commitId, sequence)
        }
    }

    /**
     * replay된 [snapshot]을 persist하고 commit [sequence]를 저장합니다.
     *
     * transaction 또는 batch primitive가 있는 backend는 snapshot row와 sequence metadata가
     * 하나의 projection unit으로 복원되도록 이 method를 override해야 합니다.
     */
    protected open fun persistProjectedSnapshot(snapshot: CdoSnapshot, sequence: Long) {
        saveSnapshot(snapshot)
        updateCommitId(snapshot.commitMetadata.id, sequence)
    }

    private fun restoreHeadAfterProjection(commitId: CommitId, sequence: Long) {
        val currentHead = when {
            headLoaded -> head
            else -> head ?: loadHeadId()
        }
        val currentHeadSequence = currentHead?.let { getSeq(it) } ?: 0L
        head = if (currentHead == null || sequence >= currentHeadSequence) {
            commitId
        } else {
            currentHead
        }
        headLoaded = true
    }

    override fun getHeadId(): CommitId? {
        if (headLoaded) {
            return head
        }
        return lock.withLock {
            if (!headLoaded) {
                head = head ?: loadHeadId()
                headLoaded = true
            }
            head
        }
    }

    private fun applyQueryParams(
        snapshots: Sequence<CdoSnapshot>,
        queryParams: QueryParams,
    ): MutableList<CdoSnapshot> {
        var results = snapshots
        if (queryParams.commitIds().isNotEmpty()) {
            results = results.filterByCommitIds(queryParams.commitIds())
        }
        if (queryParams.toCommitId().isPresent) {
            results = results.filterByToCommitId(queryParams.toCommitId().get())
        }
        if (queryParams.version().isPresent) {
            results = results.filterByVersion(queryParams.version().get())
        }
        if (queryParams.author().isPresent) {
            results = results.filterByAuthor(queryParams.author().get())
        }
        if (queryParams.from().isPresent || queryParams.to().isPresent) {
            results = results.filterByCommitDate(queryParams)
        }
        if (queryParams.changedProperties().isNotEmpty()) {
            results = results.filterByChangedPropertyNames(queryParams.changedProperties())
        }
        if (queryParams.snapshotType().isPresent) {
            results = results.filterByType(queryParams.snapshotType().get())
        }
        results = results.filterByCommitProperties(queryParams.commitProperties())

        return results
            .drop(queryParams.skip())
            .take(queryParams.limit())
            .toMutableList()
    }

    private fun getPersistedIdentifiers(
        snapshotIdentifiers: Collection<SnapshotIdentifier>,
    ): List<SnapshotIdentifier> {
        return snapshotIdentifiers
            .filter {
                contains(it.globalId) && it.version > 0 && it.version <= getSnapshotSize(it.globalId)
            }
    }
}
