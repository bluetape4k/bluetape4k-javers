package io.bluetape4k.javers.persistence.redis.repository

import com.google.gson.JsonObject
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.codec.Base58
import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.model.SnapshotEntity
import org.javers.core.repository.AbstractJaversCommitTest
import org.javers.repository.jql.QueryBuilder
import org.junit.jupiter.api.Test

abstract class AbstractRedisCdoSnapshotRepositoryParityTest: AbstractJaversCommitTest() {

    protected abstract val providerName: String

    protected abstract fun createRepository(
        repositoryName: String,
        codec: JaversCodec<ByteArray> = JaversCodecs.LZ4Fory,
    ): AbstractCdoSnapshotRepository<ByteArray>

    override fun newJavers(): Javers {
        return newJavers(createRepository(uniqueRepositoryName("commit")))
    }

    @Test
    fun `repository returns snapshots in reverse chronological order`() {
        val javers = newJavers(createRepository(uniqueRepositoryName("snapshot-order")))
        val entity = SnapshotEntity(1).apply { intProperty = 100 }

        javers.commit("author", entity)
        entity.intProperty = 101
        javers.commit("author", entity)

        val snapshots = javers.findSnapshots(QueryBuilder.byInstanceId(1, SnapshotEntity::class.java).build())

        snapshots shouldHaveSize 2
        snapshots[0].version shouldBeEqualTo 2L
        snapshots[0].getPropertyValue("intProperty") shouldBeEqualTo 101
        snapshots[1].version shouldBeEqualTo 1L
        snapshots[1].getPropertyValue("intProperty") shouldBeEqualTo 100
    }

    @Test
    fun `repository restores head commit id after rebuild`() {
        val repositoryName = uniqueRepositoryName("head-restore")
        val repository = createRepository(repositoryName)
        val javers = newJavers(repository)
        val entity = SnapshotEntity(1).apply {
            intProperty = 100
        }
        val commit = javers.commit("author", entity)

        repository.getHeadId() shouldBeEqualTo commit.id

        val rebuiltRepository = createRepository(repositoryName)
        val rebuiltJavers = newJavers(rebuiltRepository)

        rebuiltRepository.getHeadId() shouldBeEqualTo commit.id

        entity.intProperty = 101
        val nextCommit = rebuiltJavers.commit("author", entity)

        nextCommit.snapshots shouldHaveSize 1
        rebuiltRepository.getHeadId() shouldBeEqualTo nextCommit.id
    }

    @Test
    fun `failed snapshot encoding does not advance repository head`() {
        val repository = createRepository(uniqueRepositoryName("failure"), FailingCodec)
        val javers = newJavers(repository)

        assertFailsWith<RuntimeException> {
            javers.commit("author", SnapshotEntity(1))
        }

        repository.getHeadId().shouldBeNull()
    }

    private fun newJavers(repository: AbstractCdoSnapshotRepository<ByteArray>): Javers {
        return JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()
    }

    private fun uniqueRepositoryName(contract: String): String =
        "bluetape4k:$providerName:$contract:${Base58.randomString(6)}"

    private object FailingCodec: JaversCodec<ByteArray> {
        override fun encode(jsonElement: JsonObject): ByteArray {
            throw RuntimeException("codec encode failed")
        }

        override fun decode(encodedData: ByteArray): JsonObject? = JaversCodecs.Fory.decode(encodedData)
    }
}
