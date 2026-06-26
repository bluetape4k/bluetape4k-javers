package io.bluetape4k.javers.persistence.redis.repository

import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.RedisCodec
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
}
