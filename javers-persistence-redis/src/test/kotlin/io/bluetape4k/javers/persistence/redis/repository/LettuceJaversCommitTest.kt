package io.bluetape4k.javers.persistence.redis.repository

import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.RedisCodec
import org.javers.core.commit.CommitId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.javers.core.JaversBuilder
import org.javers.core.model.SnapshotEntity
import org.junit.jupiter.api.Test

class LettuceJaversCommitTest: AbstractRedisCdoSnapshotRepositoryParityTest() {

    companion object: KLogging()

    private val lettuceClient by lazy { RedisServer.Launcher.LettuceLib.getRedisClient() }

    override val providerName: String = "lettuce"

    override fun createRepository(
        repositoryName: String,
        codec: JaversCodec<ByteArray>,
    ): AbstractCdoSnapshotRepository<ByteArray> {
        return LettuceCdoSnapshotRepository(repositoryName, lettuceClient, codec)
    }

    @Test
    fun `close closes initialized repository connection without shutting down caller owned client`() {
        val client = mockk<RedisClient>(relaxed = true)
        val connection = mockk<StatefulRedisConnection<String, Any>>(relaxed = true)
        val commands = mockk<RedisCommands<String, Any>>(relaxed = true)

        every { client.connect(any<RedisCodec<String, Any>>()) } returns connection
        every { connection.sync() } returns commands
        every { commands.hgetall(any()) } returns emptyMap()

        val repository = LettuceCdoSnapshotRepository("lifecycle", client)

        repository.getHeadId()
        repository.close()
        repository.close()

        verify(exactly = 1) { client.connect(any<RedisCodec<String, Any>>()) }
        verify(exactly = 1) { connection.close() }
        verify(exactly = 0) { client.shutdown() }
    }

    @Test
    fun `close closes initialized read and write connections after commit without shutting down caller owned client`() {
        val client = mockk<RedisClient>(relaxed = true)
        val readConnection = mockk<StatefulRedisConnection<String, Any>>(relaxed = true)
        val writeConnection = mockk<StatefulRedisConnection<String, Any>>(relaxed = true)
        val readCommands = mockk<RedisCommands<String, Any>>(relaxed = true)
        val writeCommands = mockk<RedisCommands<String, Any>>(relaxed = true)

        every { client.connect(any<RedisCodec<String, Any>>()) } returnsMany listOf(readConnection, writeConnection)
        every { readConnection.sync() } returns readCommands
        every { writeConnection.sync() } returns writeCommands
        every { readCommands.hgetall(any()) } returns emptyMap()
        every { readCommands.hget(any(), any()) } returns null

        val repository = LettuceCdoSnapshotRepository("lifecycle-commit", client)
        val javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()

        javers.commit("author", SnapshotEntity(1))
        repository.close()

        verify(exactly = 2) { client.connect(any<RedisCodec<String, Any>>()) }
        verify(exactly = 1) { readConnection.close() }
        verify(exactly = 1) { writeConnection.close() }
        verify(exactly = 0) { client.shutdown() }
    }

    @Test
    fun `persist discards transaction and propagates exec failure`() {
        val client = mockk<RedisClient>(relaxed = true)
        val readConnection = mockk<StatefulRedisConnection<String, Any>>(relaxed = true)
        val writeConnection = mockk<StatefulRedisConnection<String, Any>>(relaxed = true)
        val readCommands = mockk<RedisCommands<String, Any>>(relaxed = true)
        val writeCommands = mockk<RedisCommands<String, Any>>(relaxed = true)

        every { client.connect(any<RedisCodec<String, Any>>()) } returnsMany listOf(readConnection, writeConnection)
        every { readConnection.sync() } returns readCommands
        every { writeConnection.sync() } returns writeCommands
        every { readCommands.hgetall(any()) } returns emptyMap()
        every { readCommands.hget(any(), any()) } returns null
        every { writeCommands.exec() } throws IllegalStateException("exec failed")

        val repository = LettuceCdoSnapshotRepository("transaction-failure", client)
        val javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()

        assertFailsWith<RuntimeException> {
            javers.commit("author", SnapshotEntity(30))
        }

        verify(exactly = 1) { writeCommands.multi() }
        verify(exactly = 1) { writeCommands.discard() }
    }

    @Test
    fun `saveSnapshot directly appends encoded snapshot and updates key index`() {
        val repository = LettuceCdoSnapshotRepository("direct-save", lettuceClient)
        val javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()
        val snapshot = javers.commit("author", SnapshotEntity(10)).snapshots.single()

        repository.saveSnapshot(snapshot)

        repository.loadSnapshots(snapshot.globalId.value()) shouldHaveSize 2
        repository.snapshotSize(snapshot.globalId.value()) shouldBeEqualTo 2
    }

    @Test
    fun `projectSnapshot restores snapshot and sequence metadata`() {
        val sourceRepository = LettuceCdoSnapshotRepository("projection-source", lettuceClient)
        val sourceJavers = JaversBuilder.javers()
            .registerJaversRepository(sourceRepository)
            .build()
        val snapshot = sourceJavers.commit("author", SnapshotEntity(20)).snapshots.single()

        val targetRepository = LettuceCdoSnapshotRepository("projection-target", lettuceClient)
        JaversBuilder.javers()
            .registerJaversRepository(targetRepository)
            .build()

        targetRepository.projectSnapshot(snapshot)

        targetRepository.loadSnapshots(snapshot.globalId.value()) shouldHaveSize 1
        targetRepository.getHeadId() shouldBeEqualTo snapshot.commitMetadata.id
        targetRepository.sequenceOf(snapshot.commitMetadata.id) shouldBeGreaterThan 0L
    }

    @Test
    fun `sequence metadata can be updated independently`() {
        val repositoryName = "sequence-update"
        val commitId = CommitId(9000L, 0)
        val repository = LettuceCdoSnapshotRepository(repositoryName, lettuceClient)

        repository.updateSequence(commitId, 42L)

        val rebuiltRepository = LettuceCdoSnapshotRepository(repositoryName, lettuceClient)

        rebuiltRepository.getHeadId() shouldBeEqualTo commitId
        rebuiltRepository.sequenceOf(commitId) shouldBeEqualTo 42L
    }

    private fun LettuceCdoSnapshotRepository.snapshotSize(globalIdValue: String): Int {
        val method = LettuceCdoSnapshotRepository::class.java.getDeclaredMethod("getSnapshotSize", String::class.java)
        method.isAccessible = true
        return method.invoke(this, globalIdValue) as Int
    }

    private fun LettuceCdoSnapshotRepository.updateSequence(commitId: CommitId, sequence: Long) {
        val method = LettuceCdoSnapshotRepository::class.java.getDeclaredMethod(
            "updateCommitId",
            CommitId::class.java,
            java.lang.Long.TYPE,
        )
        method.isAccessible = true
        method.invoke(this, commitId, sequence)
    }

    private fun LettuceCdoSnapshotRepository.sequenceOf(commitId: CommitId): Long {
        val method = LettuceCdoSnapshotRepository::class.java.getDeclaredMethod("getSeq", CommitId::class.java)
        method.isAccessible = true
        return method.invoke(this, commitId) as Long
    }
}
