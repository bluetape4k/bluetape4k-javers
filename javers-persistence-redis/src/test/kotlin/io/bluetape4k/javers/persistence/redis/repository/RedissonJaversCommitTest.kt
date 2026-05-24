package io.bluetape4k.javers.persistence.redis.repository

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.model.SnapshotEntity
import org.javers.core.repository.AbstractJaversCommitTest
import org.junit.jupiter.api.Test

class RedissonJaversCommitTest: AbstractJaversCommitTest() {

    companion object: KLogging()

    private val redisson by lazy { RedisServer.Launcher.RedissonLib.getRedisson() }

    override fun newJavers(): Javers {
        // NOTE: 각각의 테스트가 Javers를 매번 새롭게 만들고, Snapshot정보를 clear해야 하므로 Redis를 Flush합니다.
        redisson.keys.flushdb()

        val repository = RedissonCdoSnapshotRepository("bluetape4k:redisson", redisson)

        return newJavers(repository)
    }

    @Test
    fun `repository restores head commit id after rebuild`() {
        redisson.keys.flushdb()
        val repositoryName = "bluetape4k:redisson:head-restore"
        val repository = RedissonCdoSnapshotRepository(repositoryName, redisson)
        val javers = newJavers(repository)
        val entity = SnapshotEntity(1).apply {
            intProperty = 100
        }
        val commit = javers.commit("author", entity)

        repository.getHeadId() shouldBeEqualTo commit.id

        val rebuiltRepository = RedissonCdoSnapshotRepository(repositoryName, redisson)
        val rebuiltJavers = newJavers(rebuiltRepository)

        rebuiltRepository.getHeadId() shouldBeEqualTo commit.id

        entity.intProperty = 101
        val nextCommit = rebuiltJavers.commit("author", entity)

        nextCommit.snapshots.size shouldBeEqualTo 1
        rebuiltRepository.getHeadId() shouldBeEqualTo nextCommit.id
    }

    private fun newJavers(repository: RedissonCdoSnapshotRepository): Javers {
        return JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()
    }
}
