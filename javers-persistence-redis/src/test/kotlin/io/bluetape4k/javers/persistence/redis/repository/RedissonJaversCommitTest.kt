package io.bluetape4k.javers.persistence.redis.repository

import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer

class RedissonJaversCommitTest: AbstractRedisCdoSnapshotRepositoryParityTest() {

    companion object: KLogging()

    private val redisson by lazy { RedisServer.Launcher.RedissonLib.getRedisson() }

    override val providerName: String = "redisson"

    override fun createRepository(
        repositoryName: String,
        codec: JaversCodec<ByteArray>,
    ): AbstractCdoSnapshotRepository<ByteArray> {
        return RedissonCdoSnapshotRepository(repositoryName, redisson, codec)
    }
}
