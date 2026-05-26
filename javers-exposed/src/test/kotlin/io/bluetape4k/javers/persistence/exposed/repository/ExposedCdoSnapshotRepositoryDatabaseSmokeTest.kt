package io.bluetape4k.javers.persistence.exposed.repository

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTable
import io.bluetape4k.javers.persistence.exposed.schema.CommitTable
import org.javers.core.JaversBuilder
import org.javers.core.model.SnapshotEntity
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ExposedCdoSnapshotRepositoryDatabaseSmokeTest : AbstractExposedTest() {

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `commit and load latest snapshot on shared database matrix`(testDB: TestDB) {
        withTables(testDB, CommitTable, CdoSnapshotTable) {
            assertRepositorySmoke()
        }
    }

    private fun assertRepositorySmoke() {
        val repository = ExposedCdoSnapshotRepository()
        val javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()

        repository.ensureSchema()

        val entity = SnapshotEntity(1).apply { intProperty = 100 }
        val commit = javers.commit("author", entity)
        val latest = repository.getLatest(commit.snapshots.first().globalId).get()

        latest.version shouldBeEqualTo 1L
        latest.getPropertyValue("intProperty") shouldBeEqualTo 100
        repository.getHeadId() shouldBeEqualTo commit.id
    }
}
