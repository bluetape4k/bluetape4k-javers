package io.bluetape4k.javers.repository.composite

import io.bluetape4k.javers.repository.CdoSnapshotRepository
import org.javers.core.commit.Commit
import org.javers.core.commit.CommitId
import org.javers.core.json.JsonConverter
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.core.metamodel.`object`.GlobalId
import org.javers.core.metamodel.type.EntityType
import org.javers.core.metamodel.type.ManagedType
import org.javers.repository.api.QueryParams
import org.javers.repository.api.SnapshotIdentifier
import java.util.*

/**
 * Composite JaVers [CdoSnapshotRepository] with one durable primary repository
 * and ordered secondary fanout repositories.
 *
 * ## Behavior / Contract
 * - Public read operations delegate to [primary].
 * - [persist] writes [primary] first with its native repository semantics, then
 *   fans out to [secondaryRepositories] in order.
 * - [saveSnapshot] saves [primary] first and then ordered secondaries. It does
 *   not update repository head metadata by itself, matching the base
 *   [CdoSnapshotRepository.saveSnapshot] contract.
 * - Secondary failures are handled by [options]. The default is fail-fast.
 * - This repository is not transactional across delegates. If a secondary
 *   fails after the primary succeeds, the primary may already expose the
 *   persisted commit.
 *
 * ```kotlin
 * val composite = CompositeCdoSnapshotRepository(
 *     primary = exposedRepository,
 *     secondaryRepositories = listOf(kafkaRepository),
 * )
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(composite)
 *     .build()
 * ```
 *
 * @property primary durable read/write repository used as the source of truth
 * @property secondaryRepositories ordered fanout targets
 * @property options failure handling options
 */
class CompositeCdoSnapshotRepository private constructor(
    val primary: CdoSnapshotRepository,
    val secondaryRepositories: List<CdoSnapshotRepository>,
    val options: CompositeCdoSnapshotRepositoryOptions,
): CdoSnapshotRepository, AutoCloseable {

    companion object {

        /**
         * Creates a composite repository from a primary and a collection of secondaries.
         */
        operator fun invoke(
            primary: CdoSnapshotRepository,
            secondaryRepositories: Collection<CdoSnapshotRepository> = emptyList(),
            options: CompositeCdoSnapshotRepositoryOptions = CompositeCdoSnapshotRepositoryOptions.Default,
        ): CompositeCdoSnapshotRepository {
            require(secondaryRepositories.none { it === primary }) {
                "secondaryRepositories must not contain primary"
            }
            return CompositeCdoSnapshotRepository(
                primary = primary,
                secondaryRepositories = secondaryRepositories.toList(),
                options = options,
            )
        }

        /**
         * Creates a composite repository from a primary and vararg secondaries.
         */
        operator fun invoke(
            primary: CdoSnapshotRepository,
            vararg secondaryRepositories: CdoSnapshotRepository,
            options: CompositeCdoSnapshotRepositoryOptions = CompositeCdoSnapshotRepositoryOptions.Default,
        ): CompositeCdoSnapshotRepository =
            invoke(
                primary = primary,
                secondaryRepositories = secondaryRepositories.asList(),
                options = options,
            )
    }

    override fun setJsonConverter(jsonConverter: JsonConverter?) {
        primary.setJsonConverter(jsonConverter)
        secondaryRepositories.forEach { it.setJsonConverter(jsonConverter) }
    }

    override fun ensureSchema() {
        executePrimary("ensureSchema") {
            it.ensureSchema()
        }
        executeSecondaries("ensureSchema", options.ensureSchemaFailurePolicy) {
            it.ensureSchema()
        }
    }

    override fun getLatest(globalId: GlobalId): Optional<CdoSnapshot> =
        primary.getLatest(globalId)

    override fun getLatest(globalIds: MutableCollection<GlobalId>): MutableList<CdoSnapshot> =
        primary.getLatest(globalIds)

    override fun getStateHistory(globalId: GlobalId, queryParams: QueryParams): MutableList<CdoSnapshot> =
        primary.getStateHistory(globalId, queryParams)

    override fun getStateHistory(
        givenClasses: MutableSet<ManagedType>,
        queryParams: QueryParams,
    ): MutableList<CdoSnapshot> =
        primary.getStateHistory(givenClasses, queryParams)

    override fun getValueObjectStateHistory(
        ownerEntity: EntityType,
        path: String,
        queryParams: QueryParams,
    ): MutableList<CdoSnapshot> =
        primary.getValueObjectStateHistory(ownerEntity, path, queryParams)

    override fun getSnapshots(queryParams: QueryParams): MutableList<CdoSnapshot> =
        primary.getSnapshots(queryParams)

    override fun getSnapshots(snapshotIdentifiers: MutableCollection<SnapshotIdentifier>): List<CdoSnapshot> =
        primary.getSnapshots(snapshotIdentifiers)

    override fun persist(commit: Commit?) {
        if (commit == null) {
            return
        }

        executePrimary("persist") {
            it.persist(commit)
        }
        executeSecondaries("persist", options.writeFailurePolicy) {
            it.persist(commit)
        }
    }

    override fun getHeadId(): CommitId? =
        primary.headId

    override fun saveSnapshot(snapshot: CdoSnapshot) {
        executePrimary("saveSnapshot") {
            it.saveSnapshot(snapshot)
        }
        executeSecondaries("saveSnapshot", options.writeFailurePolicy) {
            it.saveSnapshot(snapshot)
        }
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> =
        primary.loadSnapshots(globalIdValue)

    override fun close() {
        val closeableDelegates = buildList {
            if (primary is AutoCloseable) {
                add(CompositeCdoSnapshotDelegateRole.PRIMARY to (0 to primary))
            }
            secondaryRepositories.forEachIndexed { index, repository ->
                if (repository is AutoCloseable) {
                    add(CompositeCdoSnapshotDelegateRole.SECONDARY to (index to repository))
                }
            }
        }
        executeCloseables(closeableDelegates)
    }

    private inline fun executePrimary(
        operation: String,
        block: (CdoSnapshotRepository) -> Unit,
    ) {
        try {
            block(primary)
        } catch (e: Exception) {
            throw CompositeCdoSnapshotException(
                listOf(primary.failure(CompositeCdoSnapshotDelegateRole.PRIMARY, 0, operation, e))
            )
        }
    }

    private inline fun executeSecondaries(
        operation: String,
        failurePolicy: CompositeCdoSnapshotFailurePolicy,
        block: (CdoSnapshotRepository) -> Unit,
    ) {
        val failures = mutableListOf<CompositeCdoSnapshotWriteFailure>()
        secondaryRepositories.forEachIndexed { index, repository ->
            try {
                block(repository)
            } catch (e: Exception) {
                failures += repository.failure(CompositeCdoSnapshotDelegateRole.SECONDARY, index, operation, e)
                if (failurePolicy == CompositeCdoSnapshotFailurePolicy.FAIL_FAST) {
                    throw CompositeCdoSnapshotException(failures)
                }
            }
        }
        if (failures.isNotEmpty()) {
            throw CompositeCdoSnapshotException(failures)
        }
    }

    private fun executeCloseables(
        closeableDelegates: List<Pair<CompositeCdoSnapshotDelegateRole, Pair<Int, AutoCloseable>>>,
    ) {
        val failures = mutableListOf<CompositeCdoSnapshotWriteFailure>()
        closeableDelegates.forEach { (role, indexedRepository) ->
            val (index, closeable) = indexedRepository
            try {
                closeable.close()
            } catch (e: Exception) {
                failures += closeable.failure(role, index, "close", e)
                if (options.closeFailurePolicy == CompositeCdoSnapshotFailurePolicy.FAIL_FAST) {
                    throw CompositeCdoSnapshotException(failures)
                }
            }
        }
        if (failures.isNotEmpty()) {
            throw CompositeCdoSnapshotException(failures)
        }
    }

    private fun Any.failure(
        role: CompositeCdoSnapshotDelegateRole,
        index: Int,
        operation: String,
        cause: Throwable,
    ): CompositeCdoSnapshotWriteFailure =
        CompositeCdoSnapshotWriteFailure(
            delegateRole = role,
            delegateIndex = index,
            delegateType = javaClass.name,
            operation = operation,
            cause = cause,
        )
}
