package io.bluetape4k.javers.persistence.exposed.repository

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.codec.Base58
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTable
import io.bluetape4k.javers.persistence.exposed.schema.CommitTable
import io.bluetape4k.javers.persistence.exposed.schema.ExposedJaversTableNames
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.model.SnapshotEntity
import org.javers.core.repository.AbstractJaversCommitTest
import org.javers.repository.api.QueryParamsBuilder
import org.javers.repository.api.SnapshotIdentifier
import org.javers.repository.jql.QueryBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExposedCdoSnapshotRepositoryH2Test: AbstractJaversCommitTest() {

    private val database: Database = Database.connect(
        url = "jdbc:h2:mem:javers-exposed-${Base58.randomString(8)};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
    )

    @BeforeEach
    fun beforeEach() {
        transaction(database) {
            SchemaUtils.drop(CdoSnapshotTable, CommitTable)
            SchemaUtils.create(CommitTable, CdoSnapshotTable)
        }
    }

    override fun newJavers(): Javers {
        val repository = ExposedCdoSnapshotRepository(database)
        return newJavers(repository)
    }

    @Test
    fun `commit and load latest snapshot`() {
        val repository = ExposedCdoSnapshotRepository(database)
        val javers = newJavers(repository)
        val entity = SnapshotEntity(1).apply { intProperty = 100 }

        val commit = javers.commit("author", entity)

        val latest = repository.getLatest(commit.snapshots.first().globalId).get()

        latest.version shouldBeEqualTo 1L
        latest.getPropertyValue("intProperty") shouldBeEqualTo 100
        repository.getHeadId() shouldBeEqualTo commit.id
    }

    @Test
    fun `disabled ensure schema lets external migrations own custom tables`() {
        val options = ExposedCdoSnapshotRepositoryOptions(
            tableNames = ExposedJaversTableNames(
                commitTableName = "javers_commit_${Base58.randomString(6)}",
                snapshotTableName = "javers_snapshot_${Base58.randomString(6)}",
            ),
            createSchemaOnEnsure = false,
        )
        val schema = options.newSchema()
        val repository = ExposedCdoSnapshotRepository(database = database, options = options)

        repository.ensureSchema()

        assertFailsWith<Exception> {
            transaction(database) {
                schema.commitTable.selectAll().count()
            }
        }

        transaction(database) {
            SchemaUtils.create(*schema.tables)
        }

        val javers = newJavers(repository)
        val entity = SnapshotEntity(20).apply { intProperty = 200 }
        val commit = javers.commit("author", entity)
        val latest = repository.getLatest(commit.snapshots.first().globalId).get()

        latest.version shouldBeEqualTo 1L
        latest.getPropertyValue("intProperty") shouldBeEqualTo 200
    }

    @Test
    fun `state history and JQL return reverse chronological snapshots`() {
        val javers = newJavers()
        val entity = SnapshotEntity(1).apply { intProperty = 1 }

        javers.commit("author", entity)
        entity.intProperty = 2
        javers.commit("author", entity)

        val snapshots = javers.findSnapshots(QueryBuilder.byInstanceId(1, SnapshotEntity::class.java).build())

        snapshots.size shouldBeEqualTo 2
        snapshots[0].version shouldBeEqualTo 2L
        snapshots[0].getPropertyValue("intProperty") shouldBeEqualTo 2
        snapshots[1].version shouldBeEqualTo 1L
        snapshots[1].getPropertyValue("intProperty") shouldBeEqualTo 1
    }

    @Test
    fun `repository restores head commit id after rebuild`() {
        val repository = ExposedCdoSnapshotRepository(database)
        val javers = newJavers(repository)
        val entity = SnapshotEntity(1).apply { intProperty = 100 }
        val commit = javers.commit("author", entity)

        repository.getHeadId() shouldBeEqualTo commit.id

        val rebuiltRepository = ExposedCdoSnapshotRepository(database)
        val rebuiltJavers = newJavers(rebuiltRepository)

        rebuiltRepository.getHeadId() shouldBeEqualTo commit.id

        entity.intProperty = 101
        val nextCommit = rebuiltJavers.commit("author", entity)

        nextCommit.snapshots.size shouldBeEqualTo 1
        rebuiltRepository.getHeadId() shouldBeEqualTo nextCommit.id
    }

    @Test
    fun `repository loads snapshots by identifier`() {
        val repository = ExposedCdoSnapshotRepository(database)
        val javers = newJavers(repository)
        val entity = SnapshotEntity(1).apply { intProperty = 1 }

        javers.commit("author", entity)
        entity.intProperty = 2
        val secondCommit = javers.commit("author", entity)
        val latest = secondCommit.snapshots.first()

        val snapshots = repository.getSnapshots(mutableListOf(SnapshotIdentifier(latest.globalId, 1)))

        snapshots.single().version shouldBeEqualTo 1L
        snapshots.single().getPropertyValue("intProperty") shouldBeEqualTo 1
    }

    @Test
    fun `query params filter by author`() {
        val repository = ExposedCdoSnapshotRepository(database)
        val javers = newJavers(repository)
        val entity = SnapshotEntity(1).apply { intProperty = 1 }

        javers.commit("author-a", entity)
        entity.intProperty = 2
        javers.commit("author-b", entity)

        val snapshots = repository.getSnapshots(QueryParamsBuilder.withLimit(10).author("author-b").build())

        snapshots.single().commitMetadata.author shouldBeEqualTo "author-b"
        snapshots.single().getPropertyValue("intProperty").shouldNotBeNull()
    }

    private fun newJavers(repository: ExposedCdoSnapshotRepository): Javers {
        return JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()
    }
}
