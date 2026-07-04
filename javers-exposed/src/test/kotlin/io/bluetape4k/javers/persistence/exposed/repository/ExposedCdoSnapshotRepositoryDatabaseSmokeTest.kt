package io.bluetape4k.javers.persistence.exposed.repository

import com.google.gson.JsonObject
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTable
import io.bluetape4k.javers.persistence.exposed.schema.CommitTable
import io.bluetape4k.javers.persistence.exposed.schema.ExposedJaversTableNames
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.model.DummyUser
import org.javers.core.model.SnapshotEntity
import org.javers.repository.api.QueryParamsBuilder
import org.javers.repository.api.SnapshotIdentifier
import org.javers.repository.jql.QueryBuilder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.atomic.AtomicInteger

class ExposedCdoSnapshotRepositoryDatabaseSmokeTest : AbstractExposedTest() {

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `commit and load latest snapshot on shared database matrix`(testDB: TestDB) {
        withTables(testDB, CommitTable, CdoSnapshotTable) {
            assertRepositorySmoke()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `custom table names commit and load on shared database matrix`(testDB: TestDB) {
        val options = customOptions(testDB)
        val schema = options.newSchema()

        withTables(testDB, *schema.tables) {
            val repository = ExposedCdoSnapshotRepository(options = options)
            val javers = newJavers(repository)
            val entity = SnapshotEntity(10).apply { intProperty = 100 }

            val commit = javers.commit("author", entity)
            val latest = repository.getLatest(commit.snapshots.first().globalId).get()

            schema.commitTable.tableName shouldBeEqualTo options.tableNames.commitTableName
            schema.snapshotTable.tableName shouldBeEqualTo options.tableNames.snapshotTableName
            latest.version shouldBeEqualTo 1L
            latest.getPropertyValue("intProperty") shouldBeEqualTo 100
            repository.getHeadId() shouldBeEqualTo commit.id
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `snapshot and commit tables declare natural keys and hot path indexes on shared database matrix`(testDB: TestDB) {
        withTables(testDB, CommitTable, CdoSnapshotTable) {
            val snapshotKey = CdoSnapshotTable.primaryKey
            snapshotKey.name shouldBeEqualTo CdoSnapshotTable.GLOBAL_ID_VERSION_INDEX
            snapshotKey.columns.map { it.name } shouldBeEqualTo listOf(
                CdoSnapshotTable.globalId.name,
                CdoSnapshotTable.version.name,
            )

            val commitKey = CommitTable.primaryKey
            commitKey.name shouldBeEqualTo CommitTable.COMMIT_ID_INDEX
            commitKey.columns.map { it.name } shouldBeEqualTo listOf(CommitTable.commitId.name)

            val sequenceIndex = CommitTable.indices.single { it.indexName == CommitTable.SEQUENCE_INDEX }
            sequenceIndex.unique.shouldBeFalse()
            sequenceIndex.columns shouldBeEqualTo listOf(CommitTable.sequence)
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `state history and JQL return reverse chronological snapshots on shared database matrix`(testDB: TestDB) {
        withTables(testDB, CommitTable, CdoSnapshotTable) {
            val javers = newJavers()
            val entity = SnapshotEntity(1).apply { intProperty = 1 }

            javers.commit("author", entity)
            entity.intProperty = 2
            javers.commit("author", entity)

            val snapshots = javers.findSnapshots(QueryBuilder.byInstanceId(1, SnapshotEntity::class.java).build())

            snapshots shouldHaveSize 2
            snapshots[0].version shouldBeEqualTo 2L
            snapshots[0].getPropertyValue("intProperty") shouldBeEqualTo 2
            snapshots[1].version shouldBeEqualTo 1L
            snapshots[1].getPropertyValue("intProperty") shouldBeEqualTo 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `repository restores head commit id after rebuild on shared database matrix`(testDB: TestDB) {
        withTables(testDB, CommitTable, CdoSnapshotTable) {
            val repository = ExposedCdoSnapshotRepository()
            val javers = newJavers(repository)
            val entity = SnapshotEntity(1).apply { intProperty = 100 }
            val commit = javers.commit("author", entity)

            repository.getHeadId() shouldBeEqualTo commit.id

            val rebuiltRepository = ExposedCdoSnapshotRepository()
            val rebuiltJavers = newJavers(rebuiltRepository)

            rebuiltRepository.getHeadId() shouldBeEqualTo commit.id

            entity.intProperty = 101
            val nextCommit = rebuiltJavers.commit("author", entity)

            nextCommit.snapshots shouldHaveSize 1
            rebuiltRepository.getHeadId() shouldBeEqualTo nextCommit.id
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `repository loads snapshots by identifier on shared database matrix`(testDB: TestDB) {
        withTables(testDB, CommitTable, CdoSnapshotTable) {
            val repository = ExposedCdoSnapshotRepository()
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
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `query params filter by author on shared database matrix`(testDB: TestDB) {
        withTables(testDB, CommitTable, CdoSnapshotTable) {
            val repository = ExposedCdoSnapshotRepository()
            val javers = newJavers(repository)
            val entity = SnapshotEntity(1).apply { intProperty = 1 }

            javers.commit("author-a", entity)
            entity.intProperty = 2
            javers.commit("author-b", entity)

            val snapshots = repository.getSnapshots(QueryParamsBuilder.withLimit(10).author("author-b").build())

            snapshots.single().commitMetadata.author shouldBeEqualTo "author-b"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `query params author filter uses SQL pushdown on shared database matrix`(testDB: TestDB) {
        withTables(testDB, CommitTable, CdoSnapshotTable) {
            val codec = CountingStringCodec()
            val repository = ExposedCdoSnapshotRepository(codec = codec)
            val javers = newJavers(repository)

            javers.commit("author-a", SnapshotEntity(101).apply { intProperty = 1 })
            javers.commit("author-b", SnapshotEntity(102).apply { intProperty = 2 })
            javers.commit("author-a", SnapshotEntity(103).apply { intProperty = 3 })

            codec.reset()
            val snapshots = repository.getSnapshots(QueryParamsBuilder.withLimit(10).author("author-b").build())

            snapshots.single().commitMetadata.author shouldBeEqualTo "author-b"
            codec.decodeCount.get() shouldBeEqualTo 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `query params limit bounds decoded SQL rows on shared database matrix`(testDB: TestDB) {
        withTables(testDB, CommitTable, CdoSnapshotTable) {
            val codec = CountingStringCodec()
            val repository = ExposedCdoSnapshotRepository(codec = codec)
            val javers = newJavers(repository)

            javers.commit("author-a", SnapshotEntity(111).apply { intProperty = 1 })
            javers.commit("author-b", SnapshotEntity(112).apply { intProperty = 2 })
            javers.commit("author-c", SnapshotEntity(113).apply { intProperty = 3 })

            codec.reset()
            val snapshots = repository.getSnapshots(QueryParamsBuilder.withLimit(2).build())

            snapshots shouldHaveSize 2
            codec.decodeCount.get() shouldBeEqualTo 2
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `query params skip and limit page decoded SQL rows on shared database matrix`(testDB: TestDB) {
        withTables(testDB, CommitTable, CdoSnapshotTable) {
            val codec = CountingStringCodec()
            val repository = ExposedCdoSnapshotRepository(codec = codec)
            val javers = newJavers(repository)

            javers.commit("author-a", SnapshotEntity(121).apply { intProperty = 1 })
            javers.commit("author-b", SnapshotEntity(122).apply { intProperty = 2 })
            javers.commit("author-c", SnapshotEntity(123).apply { intProperty = 3 })

            codec.reset()
            val snapshots = repository.getSnapshots(QueryParamsBuilder.withLimit(1).skip(1).build())

            snapshots.single().getPropertyValue("intProperty") shouldBeEqualTo 2
            codec.decodeCount.get() shouldBeEqualTo 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `class state history uses managed type SQL pushdown on shared database matrix`(testDB: TestDB) {
        withTables(testDB, CommitTable, CdoSnapshotTable) {
            val codec = CountingStringCodec()
            val repository = ExposedCdoSnapshotRepository(codec = codec)
            val javers = newJavers(repository)

            javers.commit("author", SnapshotEntity(201).apply { intProperty = 1 })
            javers.commit("author", DummyUser("user-201").apply { age = 42 })

            codec.reset()
            val snapshots = javers.findSnapshots(QueryBuilder.byClass(SnapshotEntity::class.java).limit(10).build())

            snapshots.single().globalId.value() shouldBeEqualTo "org.javers.core.model.SnapshotEntity/201"
            codec.decodeCount.get() shouldBeEqualTo 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `unsupported changed property query keeps fallback behavior on shared database matrix`(testDB: TestDB) {
        withTables(testDB, CommitTable, CdoSnapshotTable) {
            val repository = ExposedCdoSnapshotRepository()
            val javers = newJavers(repository)
            val entity = SnapshotEntity(301).apply { intProperty = 1 }

            javers.commit("author", entity)
            entity.intProperty = 2
            javers.commit("author", entity)

            val snapshots = repository.getSnapshots(
                QueryParamsBuilder.withLimit(10)
                    .changedProperties(listOf("intProperty"))
                    .build(),
            )

            snapshots shouldHaveSize 2
            snapshots.first().version shouldBeEqualTo 2L
        }
    }

    private fun assertRepositorySmoke() {
        val repository = ExposedCdoSnapshotRepository()
        val javers = newJavers(repository)

        repository.ensureSchema()

        val entity = SnapshotEntity(1).apply { intProperty = 100 }
        val commit = javers.commit("author", entity)
        val latest = repository.getLatest(commit.snapshots.first().globalId).get()

        latest.version shouldBeEqualTo 1L
        latest.getPropertyValue("intProperty") shouldBeEqualTo 100
        repository.getHeadId() shouldBeEqualTo commit.id
    }

    private fun newJavers(repository: ExposedCdoSnapshotRepository = ExposedCdoSnapshotRepository()): Javers {
        return JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()
    }

    private fun customOptions(testDB: TestDB): ExposedCdoSnapshotRepositoryOptions {
        val suffix = "${testDB.name.lowercase()}_${Base58.randomString(6)}"
        return ExposedCdoSnapshotRepositoryOptions(
            tableNames = ExposedJaversTableNames(
                commitTableName = "javers_commit_$suffix",
                snapshotTableName = "javers_snapshot_$suffix",
            ),
        )
    }

    private class CountingStringCodec: JaversCodec<String> {

        private val delegate = JaversCodecs.String
        val decodeCount: AtomicInteger = AtomicInteger()

        override fun encode(jsonElement: JsonObject): String {
            return delegate.encode(jsonElement)
        }

        override fun decode(encodedData: String): JsonObject? {
            decodeCount.incrementAndGet()
            return delegate.decode(encodedData)
        }

        fun reset() {
            decodeCount.set(0)
        }
    }
}
