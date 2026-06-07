package io.bluetape4k.javers.persistence.redis.repository

import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer

class LettuceJaversCommitTest: AbstractRedisCdoSnapshotRepositoryParityTest() {

    companion object: KLogging()

    private val lettuceClient by lazy { RedisServer.Launcher.LettuceLib.getRedisClient() }

    override val providerName: String = "lettuce"

    override fun flushRedis() {
        lettuceClient.connect().use {
            it.sync().flushdb()
        }
    }

    override fun createRepository(
        repositoryName: String,
        codec: JaversCodec<ByteArray>,
    ): AbstractCdoSnapshotRepository<ByteArray> {
        return LettuceCdoSnapshotRepository(repositoryName, lettuceClient, codec)
    }
}
