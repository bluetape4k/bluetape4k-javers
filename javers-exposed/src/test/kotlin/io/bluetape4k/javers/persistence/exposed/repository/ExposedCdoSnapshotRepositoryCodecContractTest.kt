package io.bluetape4k.javers.persistence.exposed.repository

import com.google.gson.JsonObject
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTable
import io.bluetape4k.javers.persistence.exposed.schema.CommitTable
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import org.javers.core.JaversBuilder
import org.javers.core.model.SnapshotEntity
import org.javers.repository.jql.QueryBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import java.util.UUID

class ExposedCdoSnapshotRepositoryCodecContractTest {

    @Test
    fun `default repository codec is plain string json`() {
        withDatabase { database ->
            val repository = ExposedCdoSnapshotRepository(database)
            val javers = newJavers(repository)

            repository.codec() shouldBeEqualTo JaversCodecs.String

            javers.commit("codec-contract", SnapshotEntity(1).apply { intProperty = 1 })

            val state = states(database).single()

            state.startsWith("{").shouldBeTrue()
            JaversCodecs.String.decode(state).shouldNotBeNull()
        }
    }

    @Test
    fun `repository round trips snapshots with custom string codec`() {
        withDatabase { database ->
            val repository = ExposedCdoSnapshotRepository(database, PrefixStringCodec())
            val javers = newJavers(repository)
            val entity = SnapshotEntity(2).apply { intProperty = 1 }

            javers.commit("codec-contract", entity)
            entity.intProperty = 2
            javers.commit("codec-contract", entity)

            val states = states(database)
            states shouldHaveSize 2
            states.all { it.startsWith(PrefixStringCodec.PREFIX) }.shouldBeTrue()

            val snapshots = javers.findSnapshots(QueryBuilder.byInstanceId(2, SnapshotEntity::class.java).build())

            snapshots shouldHaveSize 2
            snapshots[0].version shouldBeEqualTo 2L
            snapshots[0].getPropertyValue("intProperty") shouldBeEqualTo 2
            snapshots[1].version shouldBeEqualTo 1L
            snapshots[1].getPropertyValue("intProperty") shouldBeEqualTo 1
        }
    }

    private fun newDatabase(): Database {
        val database = Database.connect(
            url = "jdbc:h2:mem:javers-codec-contract-${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
        transaction(database) {
            SchemaUtils.create(CommitTable, CdoSnapshotTable)
        }
        return database
    }

    private fun <T> withDatabase(block: (Database) -> T): T {
        val database = newDatabase()
        try {
            return block(database)
        } finally {
            transaction(database) {
                SchemaUtils.drop(CdoSnapshotTable, CommitTable)
            }
        }
    }

    private fun newJavers(repository: ExposedCdoSnapshotRepository) =
        JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()

    private fun states(database: Database): List<String> = transaction(database) {
        CdoSnapshotTable.selectAll().map { it[CdoSnapshotTable.state] }
    }

    private fun AbstractCdoSnapshotRepository<*>.codec(): Any {
        val field = AbstractCdoSnapshotRepository::class.java.getDeclaredField("codec")
        field.isAccessible = true
        return field.get(this)
    }

    private class PrefixStringCodec: JaversCodec<String> {
        override fun encode(jsonElement: JsonObject): String {
            return PREFIX + JaversCodecs.String.encode(jsonElement)
        }

        override fun decode(encodedData: String): JsonObject? {
            if (!encodedData.startsWith(PREFIX)) {
                return null
            }
            return JaversCodecs.String.decode(encodedData.removePrefix(PREFIX))
        }

        companion object {
            const val PREFIX = "codec-contract:"
        }
    }
}
