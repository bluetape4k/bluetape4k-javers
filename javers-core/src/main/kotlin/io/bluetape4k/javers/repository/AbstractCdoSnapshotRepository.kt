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
 * Abstract class providing common implementation for [CdoSnapshotRepository].
 *
 * ## Behavior / Contract
 * - Uses a [JaversCodec] to convert [CdoSnapshot] ↔ encoded format [T].
 * - [persist] is thread-safe via a lock, assigns a [Snowflake]-based sequence number,
 *   and delegates the whole commit write to [persistCommit].
 * - [projectSnapshot] restores decoded replay snapshots together with commit
 *   head and sequence metadata.
 * - Common [QueryParams]-based filtering (author, date, version, commitId, etc.) is handled here.
 * - Subclasses must implement [getKeys], [contains], [getSeq], [updateCommitId],
 *   [getSnapshotSize], [saveSnapshot], and [loadSnapshots].
 * - Persistent subclasses may override [loadHeadId] to restore the latest persisted
 *   head commit after a repository rebuild.
 *
 * @param T the type of encoded snapshot data (e.g. String, ByteArray)
 * @property codec the [JaversCodec] used to encode/decode snapshots
 * @property commitIdSupplier the [Snowflake] used to generate commit sequence numbers
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
     * Restores the latest head commit from persistent storage.
     *
     * Return `null` when the repository is empty or does not support head restoration.
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
     * Loads all snapshots from the repository, sorted by sequence number in descending order.
     *
     * ## OOM Warning
     * This method materializes every snapshot into memory before filtering.
     * For large repositories this is an unbounded memory allocation.
     * When the key count exceeds 10,000, a warning is logged.
     * Prefer JQL query-based access ([getSnapshots], [getStateHistory]) for production use.
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
        // Nothing to do.
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
     * Persists every snapshot in [commit] and stores the commit [sequence].
     *
     * ## Contract
     * The default implementation preserves the original per-snapshot write
     * behavior. Durable repositories that can provide commit-level atomicity
     * should override this method and wrap the whole operation in the backend's
     * transaction or batch primitive.
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
     * Persists a replayed [snapshot] and stores the commit [sequence].
     *
     * Backends with transaction or batch primitives should override this method
     * so the snapshot row and sequence metadata are restored as one projection
     * unit.
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
