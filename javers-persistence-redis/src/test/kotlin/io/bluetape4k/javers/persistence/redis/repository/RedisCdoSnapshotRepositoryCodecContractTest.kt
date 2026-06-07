package io.bluetape4k.javers.persistence.redis.repository

import com.google.gson.JsonObject
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.codec.Base58
import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.testcontainers.storage.RedisServer
import org.javers.core.JaversBuilder
import org.javers.core.model.SnapshotEntity
import org.javers.repository.jql.QueryBuilder
import org.junit.jupiter.api.Test

class RedisCdoSnapshotRepositoryCodecContractTest {

    private val lettuceClient by lazy { RedisServer.Launcher.LettuceLib.getRedisClient() }
    private val redisson by lazy { RedisServer.Launcher.RedissonLib.getRedisson() }

    @Test
    fun `lettuce repository defaults to LZ4 Fory codec`() {
        val repository = LettuceCdoSnapshotRepository(repositoryName("lettuce", "default"), lettuceClient)

        repository.codec() shouldBeEqualTo JaversCodecs.LZ4Fory

        assertSnapshotRoundTrip(repository, 1)
    }

    @Test
    fun `lettuce repository round trips snapshots with custom binary codec`() {
        val codec = TrackingBinaryCodec()
        val repository = LettuceCdoSnapshotRepository(repositoryName("lettuce", "custom"), lettuceClient, codec)

        assertSnapshotRoundTrip(repository, 2)

        codec.encodeCount shouldBeGreaterThan 0
        codec.decodeCount shouldBeGreaterThan 0
    }

    @Test
    fun `redisson repository defaults to LZ4 Fory codec`() {
        val repository = RedissonCdoSnapshotRepository(repositoryName("redisson", "default"), redisson)

        repository.codec() shouldBeEqualTo JaversCodecs.LZ4Fory

        assertSnapshotRoundTrip(repository, 3)
    }

    @Test
    fun `redisson repository round trips snapshots with custom binary codec`() {
        val codec = TrackingBinaryCodec()
        val repository = RedissonCdoSnapshotRepository(repositoryName("redisson", "custom"), redisson, codec)

        assertSnapshotRoundTrip(repository, 4)

        codec.encodeCount shouldBeGreaterThan 0
        codec.decodeCount shouldBeGreaterThan 0
    }

    private fun assertSnapshotRoundTrip(
        repository: AbstractCdoSnapshotRepository<ByteArray>,
        id: Int,
    ) {
        val javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()
        val entity = SnapshotEntity(id).apply { intProperty = 1 }

        javers.commit("codec-contract", entity)
        entity.intProperty = 2
        javers.commit("codec-contract", entity)

        val snapshots = javers.findSnapshots(QueryBuilder.byInstanceId(id, SnapshotEntity::class.java).build())

        snapshots shouldHaveSize 2
        snapshots[0].version shouldBeEqualTo 2L
        snapshots[0].getPropertyValue("intProperty") shouldBeEqualTo 2
        snapshots[1].version shouldBeEqualTo 1L
        snapshots[1].getPropertyValue("intProperty") shouldBeEqualTo 1
    }

    private fun repositoryName(client: String, contract: String): String =
        "codec-contract:$client:$contract:${Base58.randomString(12)}"

    private fun AbstractCdoSnapshotRepository<*>.codec(): Any {
        val field = AbstractCdoSnapshotRepository::class.java.getDeclaredField("codec")
        field.isAccessible = true
        return field.get(this)
    }

    private class TrackingBinaryCodec: JaversCodec<ByteArray> {
        var encodeCount: Int = 0
            private set
        var decodeCount: Int = 0
            private set

        override fun encode(jsonElement: JsonObject): ByteArray {
            encodeCount++
            return JaversCodecs.Fory.encode(jsonElement)
        }

        override fun decode(encodedData: ByteArray): JsonObject? {
            decodeCount++
            return JaversCodecs.Fory.decode(encodedData)
        }
    }
}
