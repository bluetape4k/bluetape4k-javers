package io.bluetape4k.javers.repository.composite

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.javers.repository.CdoSnapshotRepository
import io.bluetape4k.javers.repository.caffeine.CaffeineCdoSnapshotRepository
import org.javers.core.JaversBuilder
import org.javers.core.commit.Commit
import org.javers.core.commit.CommitId
import org.javers.core.json.JsonConverter
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.core.metamodel.`object`.GlobalId
import org.javers.core.metamodel.type.EntityType
import org.javers.core.metamodel.type.ManagedType
import org.javers.core.model.SnapshotEntity
import org.javers.repository.api.QueryParams
import org.javers.repository.api.SnapshotIdentifier
import org.javers.repository.jql.QueryBuilder
import org.junit.jupiter.api.Test
import java.util.*

class CompositeCdoSnapshotRepositoryTest {

    @Test
    fun `options use fail-fast writes and best-effort close by default`() {
        val options = CompositeCdoSnapshotRepositoryOptions()

        options.writeFailurePolicy shouldBeEqualTo CompositeCdoSnapshotFailurePolicy.FAIL_FAST
        options.ensureSchemaFailurePolicy shouldBeEqualTo CompositeCdoSnapshotFailurePolicy.FAIL_FAST
        options.closeFailurePolicy shouldBeEqualTo CompositeCdoSnapshotFailurePolicy.BEST_EFFORT
    }

    @Test
    fun `primary cannot also be a secondary repository`() {
        val primary = RecordingCdoSnapshotRepository("primary")

        assertFailsWith<IllegalArgumentException> {
            CompositeCdoSnapshotRepository(
                primary = primary,
                secondaryRepositories = listOf(primary),
            )
        }
    }

    @Test
    fun `read operations delegate to primary repository`() {
        val snapshot = committedSnapshot(1)
        val primary = RecordingCdoSnapshotRepository("primary").apply {
            snapshots += snapshot
            recordedHeadId = snapshot.commitId
        }
        val secondary = RecordingCdoSnapshotRepository("secondary")
        val composite = CompositeCdoSnapshotRepository(primary, secondary)

        composite.loadSnapshots(snapshot.globalId.value()) shouldBeEqualTo listOf(snapshot)
        composite.getLatest(snapshot.globalId).get() shouldBeSameInstanceAs snapshot
        composite.getHeadId() shouldBeEqualTo snapshot.commitId

        secondary.operations.shouldNotContain("loadSnapshots")
    }

    @Test
    fun `setJsonConverter propagates to all delegates`() {
        val primary = RecordingCdoSnapshotRepository("primary")
        val secondary1 = RecordingCdoSnapshotRepository("secondary1")
        val secondary2 = RecordingCdoSnapshotRepository("secondary2")
        val converter = JaversBuilder.javers().build().jsonConverter
        val composite = CompositeCdoSnapshotRepository(
            primary = primary,
            secondaryRepositories = listOf(secondary1, secondary2),
        )

        composite.setJsonConverter(converter)

        primary.recordedJsonConverter shouldBeSameInstanceAs converter
        secondary1.recordedJsonConverter shouldBeSameInstanceAs converter
        secondary2.recordedJsonConverter shouldBeSameInstanceAs converter
    }

    @Test
    fun `ensureSchema propagates primary first then secondaries`() {
        val operations = mutableListOf<String>()
        val primary = RecordingCdoSnapshotRepository("primary", operations)
        val secondary1 = RecordingCdoSnapshotRepository("secondary1", operations)
        val secondary2 = RecordingCdoSnapshotRepository("secondary2", operations)
        val composite = CompositeCdoSnapshotRepository(
            primary = primary,
            secondaryRepositories = listOf(secondary1, secondary2),
        )

        composite.ensureSchema()

        operations shouldBeEqualTo listOf("primary:ensureSchema", "secondary1:ensureSchema", "secondary2:ensureSchema")
    }

    @Test
    fun `saveSnapshot writes primary before secondaries`() {
        val operations = mutableListOf<String>()
        val primary = RecordingCdoSnapshotRepository("primary", operations)
        val secondary1 = RecordingCdoSnapshotRepository("secondary1", operations)
        val secondary2 = RecordingCdoSnapshotRepository("secondary2", operations)
        val snapshot = committedSnapshot(1)
        val composite = CompositeCdoSnapshotRepository(
            primary = primary,
            secondaryRepositories = listOf(secondary1, secondary2),
        )

        composite.saveSnapshot(snapshot)

        operations shouldBeEqualTo listOf("primary:saveSnapshot", "secondary1:saveSnapshot", "secondary2:saveSnapshot")
        primary.snapshots shouldBeEqualTo listOf(snapshot)
        secondary1.snapshots shouldBeEqualTo listOf(snapshot)
        secondary2.snapshots shouldBeEqualTo listOf(snapshot)
    }

    @Test
    fun `primary failure prevents secondary writes`() {
        val operations = mutableListOf<String>()
        val primary = RecordingCdoSnapshotRepository("primary", operations, failOn = setOf("saveSnapshot"))
        val secondary = RecordingCdoSnapshotRepository("secondary", operations)
        val composite = CompositeCdoSnapshotRepository(primary, secondary)

        val failure = assertFailsWith<CompositeCdoSnapshotException> {
            composite.saveSnapshot(committedSnapshot(1))
        }

        failure.failures.shouldHaveSize(1)
        failure.failures[0].delegateRole shouldBeEqualTo CompositeCdoSnapshotDelegateRole.PRIMARY
        operations shouldBeEqualTo listOf("primary:saveSnapshot")
    }

    @Test
    fun `exception rejects empty failures and keeps an immutable failure snapshot`() {
        assertFailsWith<IllegalArgumentException> {
            CompositeCdoSnapshotException(emptyList())
        }

        val failures = mutableListOf(delegateFailure(index = 0))
        val exception = CompositeCdoSnapshotException(failures)

        failures += delegateFailure(index = 1)

        exception.failures.shouldHaveSize(1)
        exception.cause shouldBeSameInstanceAs exception.failures[0].cause
    }

    @Test
    fun `fail-fast secondary failure stops later secondaries`() {
        val operations = mutableListOf<String>()
        val primary = RecordingCdoSnapshotRepository("primary", operations)
        val secondary1 = RecordingCdoSnapshotRepository("secondary1", operations, failOn = setOf("saveSnapshot"))
        val secondary2 = RecordingCdoSnapshotRepository("secondary2", operations)
        val composite = CompositeCdoSnapshotRepository(
            primary = primary,
            secondaryRepositories = listOf(secondary1, secondary2),
        )

        val failure = assertFailsWith<CompositeCdoSnapshotException> {
            composite.saveSnapshot(committedSnapshot(1))
        }

        failure.failures.shouldHaveSize(1)
        failure.failures[0].delegateRole shouldBeEqualTo CompositeCdoSnapshotDelegateRole.SECONDARY
        failure.failures[0].delegateIndex shouldBeEqualTo 0
        operations shouldBeEqualTo listOf("primary:saveSnapshot", "secondary1:saveSnapshot")
    }

    @Test
    fun `best-effort secondary failure attempts all secondaries and aggregates failures`() {
        val operations = mutableListOf<String>()
        val primary = RecordingCdoSnapshotRepository("primary", operations)
        val secondary1 = RecordingCdoSnapshotRepository("secondary1", operations, failOn = setOf("saveSnapshot"))
        val secondary2 = RecordingCdoSnapshotRepository("secondary2", operations)
        val secondary3 = RecordingCdoSnapshotRepository("secondary3", operations, failOn = setOf("saveSnapshot"))
        val composite = CompositeCdoSnapshotRepository(
            primary = primary,
            secondaryRepositories = listOf(secondary1, secondary2, secondary3),
            options = CompositeCdoSnapshotRepositoryOptions(
                writeFailurePolicy = CompositeCdoSnapshotFailurePolicy.BEST_EFFORT,
            ),
        )

        val failure = assertFailsWith<CompositeCdoSnapshotException> {
            composite.saveSnapshot(committedSnapshot(1))
        }

        failure.failures.shouldHaveSize(2)
        failure.failures.map { it.delegateIndex } shouldBeEqualTo listOf(0, 2)
        operations shouldBeEqualTo listOf(
            "primary:saveSnapshot",
            "secondary1:saveSnapshot",
            "secondary2:saveSnapshot",
            "secondary3:saveSnapshot",
        )
    }

    @Test
    fun `persist writes primary first and fans out commit to secondaries`() {
        val operations = mutableListOf<String>()
        val primary = RecordingCdoSnapshotRepository("primary", operations)
        val secondary = RecordingCdoSnapshotRepository("secondary", operations)
        val composite = CompositeCdoSnapshotRepository(primary, secondary)
        val javers = JaversBuilder.javers()
            .registerJaversRepository(composite)
            .build()

        javers.commit("composite", SnapshotEntity(1).apply { intProperty = 7 })

        operations shouldContain "primary:setJsonConverter"
        operations shouldContain "secondary:setJsonConverter"
        operations shouldContain "primary:persist"
        operations shouldContain "secondary:persist"
        primary.snapshots.shouldHaveSize(1)
        secondary.snapshots.shouldHaveSize(1)
        composite.getHeadId() shouldBeEqualTo primary.recordedHeadId
    }

    @Test
    fun `javers reads from primary after composite commit`() {
        val primary = CaffeineCdoSnapshotRepository()
        val secondary = RecordingCdoSnapshotRepository("secondary")
        val composite = CompositeCdoSnapshotRepository(primary, secondary)
        val javers = JaversBuilder.javers()
            .registerJaversRepository(composite)
            .build()
        val entity = SnapshotEntity(1).apply { intProperty = 1 }

        javers.commit("composite", entity)
        entity.intProperty = 2
        javers.commit("composite", entity)

        val snapshots = javers.findSnapshots(QueryBuilder.byInstanceId(1, SnapshotEntity::class.java).build())

        snapshots.shouldHaveSize(2)
        snapshots[0].version shouldBeEqualTo 2L
        snapshots[1].version shouldBeEqualTo 1L
        secondary.snapshots.shouldHaveSize(2)
    }

    @Test
    fun `close attempts every closeable delegate and aggregates failures`() {
        val operations = mutableListOf<String>()
        val primary = RecordingCdoSnapshotRepository("primary", operations, failOn = setOf("close"))
        val secondary1 = RecordingCdoSnapshotRepository("secondary1", operations, failOn = setOf("close"))
        val secondary2 = RecordingCdoSnapshotRepository("secondary2", operations)
        val composite = CompositeCdoSnapshotRepository(
            primary = primary,
            secondaryRepositories = listOf(secondary1, secondary2),
        )

        val failure = assertFailsWith<CompositeCdoSnapshotException> {
            composite.close()
        }

        failure.failures.shouldHaveSize(2)
        operations shouldBeEqualTo listOf("primary:close", "secondary1:close", "secondary2:close")
    }

    private fun committedSnapshot(id: Int): CdoSnapshot {
        val repository = CaffeineCdoSnapshotRepository()
        val javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()

        javers.commit("snapshot", SnapshotEntity(id))

        return repository.loadSnapshots("org.javers.core.model.SnapshotEntity/$id").first()
    }

    private fun delegateFailure(index: Int): CompositeCdoSnapshotWriteFailure =
        CompositeCdoSnapshotWriteFailure(
            delegateRole = CompositeCdoSnapshotDelegateRole.SECONDARY,
            delegateIndex = index,
            delegateType = RecordingCdoSnapshotRepository::class.java.name,
            operation = "saveSnapshot",
            cause = IllegalStateException("delegate $index failed"),
        )

    private class RecordingCdoSnapshotRepository(
        private val name: String,
        val operations: MutableList<String> = mutableListOf(),
        private val failOn: Set<String> = emptySet(),
    ): CdoSnapshotRepository, AutoCloseable {

        val snapshots: MutableList<CdoSnapshot> = mutableListOf()
        var recordedJsonConverter: JsonConverter? = null
        var recordedHeadId: CommitId? = null

        override fun setJsonConverter(jsonConverter: JsonConverter?) {
            operations += "$name:setJsonConverter"
            this.recordedJsonConverter = jsonConverter
        }

        override fun ensureSchema() {
            recordOrThrow("ensureSchema")
        }

        override fun getLatest(globalId: GlobalId): Optional<CdoSnapshot> =
            Optional.ofNullable(loadSnapshots(globalId).firstOrNull())

        override fun getLatest(globalIds: MutableCollection<GlobalId>): MutableList<CdoSnapshot> =
            globalIds.mapNotNull { getLatest(it).orElse(null) }.toMutableList()

        override fun getStateHistory(globalId: GlobalId, queryParams: QueryParams): MutableList<CdoSnapshot> =
            loadSnapshots(globalId).toMutableList()

        override fun getStateHistory(
            givenClasses: MutableSet<ManagedType>,
            queryParams: QueryParams,
        ): MutableList<CdoSnapshot> =
            snapshots.toMutableList()

        override fun getValueObjectStateHistory(
            ownerEntity: EntityType,
            path: String,
            queryParams: QueryParams,
        ): MutableList<CdoSnapshot> =
            mutableListOf()

        override fun getSnapshots(queryParams: QueryParams): MutableList<CdoSnapshot> =
            snapshots.toMutableList()

        override fun getSnapshots(snapshotIdentifiers: MutableCollection<SnapshotIdentifier>): List<CdoSnapshot> =
            snapshotIdentifiers.mapNotNull { identifier ->
                loadSnapshots(identifier.globalId)
                    .firstOrNull { it.version == identifier.version }
            }

        override fun persist(commit: Commit?) {
            recordOrThrow("persist")
            commit ?: return
            commit.snapshots.forEach { saveCommittedSnapshot(it) }
            recordedHeadId = commit.id
        }

        override fun getHeadId(): CommitId? =
            recordedHeadId

        override fun saveSnapshot(snapshot: CdoSnapshot) {
            recordOrThrow("saveSnapshot")
            saveCommittedSnapshot(snapshot)
        }

        override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> {
            operations += "$name:loadSnapshots"
            return snapshots
                .filter { it.globalId.value() == globalIdValue }
                .sortedByDescending { it.version }
        }

        override fun close() {
            recordOrThrow("close")
        }

        private fun saveCommittedSnapshot(snapshot: CdoSnapshot) {
            snapshots += snapshot
        }

        private fun recordOrThrow(operation: String) {
            operations += "$name:$operation"
            if (operation in failOn) {
                throw IllegalStateException("$name failed $operation")
            }
        }
    }
}
