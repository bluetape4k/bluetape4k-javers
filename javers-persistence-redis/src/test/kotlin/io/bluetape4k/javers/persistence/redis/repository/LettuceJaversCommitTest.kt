package io.bluetape4k.javers.persistence.redis.repository

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.model.SnapshotEntity
import org.javers.core.repository.AbstractJaversCommitTest
import org.junit.jupiter.api.Test

class LettuceJaversCommitTest: AbstractJaversCommitTest() {

    companion object: KLogging()

    private val lettuceClient by lazy { RedisServer.Launcher.LettuceLib.getRedisClient() }

    override fun newJavers(): Javers {
        // NOTE: 각각의 테스트가 Javers를 매번 새롭게 만들고, Snapshot정보를 clear해야 하므로 Redis를 Flush합니다.
        lettuceClient.connect().sync().flushdb()

        val repository = LettuceCdoSnapshotRepository("bluetape4k:lettuce", lettuceClient)

        return newJavers(repository)
    }

    @Test
    fun `repository restores head commit id after rebuild`() {
        val repositoryName = "bluetape4k:lettuce:head-restore:${System.nanoTime()}"
        val repository = LettuceCdoSnapshotRepository(repositoryName, lettuceClient)
        val javers = newJavers(repository)
        val commit = javers.commit("author", SnapshotEntity(1))

        repository.getHeadId() shouldBeEqualTo commit.id

        val rebuiltRepository = LettuceCdoSnapshotRepository(repositoryName, lettuceClient)
        newJavers(rebuiltRepository)

        rebuiltRepository.getHeadId() shouldBeEqualTo commit.id
    }

    private fun newJavers(repository: LettuceCdoSnapshotRepository): Javers {
        return JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()
    }
}
