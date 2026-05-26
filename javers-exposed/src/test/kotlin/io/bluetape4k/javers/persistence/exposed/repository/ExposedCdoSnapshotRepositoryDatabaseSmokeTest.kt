package io.bluetape4k.javers.persistence.exposed.repository

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTable
import io.bluetape4k.javers.persistence.exposed.schema.CommitTable
import org.javers.core.JaversBuilder
import org.javers.core.model.SnapshotEntity
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.postgresql.PostgreSQLContainer

class ExposedCdoSnapshotRepositoryDatabaseSmokeTest {

    @Test
    fun `commit and load latest snapshot on PostgreSQL`() {
        val container = PostgreSQLContainer("postgres:16-alpine")
        try {
            container.start()
            assertRepositorySmoke(container.jdbcUrl, container.driverClassName, container.username, container.password)
        } finally {
            container.stop()
        }
    }

    @Test
    fun `commit and load latest snapshot on MySQL`() {
        val container = MySQLContainer("mysql:8.4")
        try {
            container.start()
            assertRepositorySmoke(container.jdbcUrl, container.driverClassName, container.username, container.password)
        } finally {
            container.stop()
        }
    }

    private fun assertRepositorySmoke(
        url: String,
        driver: String,
        user: String,
        password: String,
    ) {
        val database = Database.connect(url = url, driver = driver, user = user, password = password)
        val repository = ExposedCdoSnapshotRepository(database)
        val javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()

        transaction(database) {
            SchemaUtils.drop(CdoSnapshotTable, CommitTable)
        }
        repository.ensureSchema()

        val entity = SnapshotEntity(1).apply { intProperty = 100 }
        val commit = javers.commit("author", entity)
        val latest = repository.getLatest(commit.snapshots.first().globalId).get()

        latest.version shouldBeEqualTo 1L
        latest.getPropertyValue("intProperty") shouldBeEqualTo 100
        repository.getHeadId() shouldBeEqualTo commit.id
    }

}
