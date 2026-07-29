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
 * 하나의 durable primary repository와 순서가 있는 secondary fanout repository를 묶는
 * composite JaVers [CdoSnapshotRepository]입니다.
 *
 * ## 동작 / 계약
 * - Public read operation은 [primary]에 위임합니다.
 * - [persist]는 [primary]를 native repository semantics로 먼저 write한 뒤,
 *   [secondaryRepositories]에 순서대로 fanout합니다.
 * - [saveSnapshot]은 [primary]를 먼저 저장하고 이후 순서대로 secondary를 저장합니다.
 *   기본 [CdoSnapshotRepository.saveSnapshot] 계약과 맞추기 위해 repository head metadata를 직접 갱신하지 않습니다.
 * - Secondary failure는 [options]에 따라 처리합니다. 기본값은 fail-fast입니다.
 * - 이 repository는 delegate 전체에 대해 transactional하지 않습니다. primary 성공 후 secondary가 실패하면
 *   primary는 이미 persisted commit을 노출할 수 있습니다.
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
 * @property primary source of truth로 사용하는 durable read/write repository입니다.
 * @property secondaryRepositories 순서가 있는 fanout target repository 목록입니다.
 * @property options failure handling option입니다.
 */
class CompositeCdoSnapshotRepository private constructor(
    val primary: CdoSnapshotRepository,
    val secondaryRepositories: List<CdoSnapshotRepository>,
    val options: CompositeCdoSnapshotRepositoryOptions,
): CdoSnapshotRepository, AutoCloseable {

    companion object {

        /**
         * primary와 secondary collection으로 composite repository를 생성합니다.
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
         * primary와 vararg secondary로 composite repository를 생성합니다.
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
                add(CompositeCdoSnapshotDelegateKind.PRIMARY to (0 to primary))
            }
            secondaryRepositories.forEachIndexed { index, repository ->
                if (repository is AutoCloseable) {
                    add(CompositeCdoSnapshotDelegateKind.SECONDARY to (index to repository))
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
                listOf(primary.failure(CompositeCdoSnapshotDelegateKind.PRIMARY, 0, operation, e))
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
                failures += repository.failure(CompositeCdoSnapshotDelegateKind.SECONDARY, index, operation, e)
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
        closeableDelegates: List<Pair<CompositeCdoSnapshotDelegateKind, Pair<Int, AutoCloseable>>>,
    ) {
        val failures = mutableListOf<CompositeCdoSnapshotWriteFailure>()
        closeableDelegates.forEach { (kind, indexedRepository) ->
            val (index, closeable) = indexedRepository
            try {
                closeable.close()
            } catch (e: Exception) {
                failures += closeable.failure(kind, index, "close", e)
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
        kind: CompositeCdoSnapshotDelegateKind,
        index: Int,
        operation: String,
        cause: Throwable,
    ): CompositeCdoSnapshotWriteFailure =
        CompositeCdoSnapshotWriteFailure(
            delegateKind = kind,
            delegateIndex = index,
            delegateClassName = javaClass.name,
            operation = operation,
            cause = cause,
        )
}
