package io.bluetape4k.javers.persistence.redis.repository

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.codec.Base58
import io.bluetape4k.javers.persistence.redis.AbstractJaversTest
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import org.javers.core.JaversBuilder
import org.javers.core.commit.CommitId
import org.javers.core.model.SnapshotEntity
import org.junit.jupiter.api.Test
import org.redisson.client.codec.LongCodec
import org.redisson.client.codec.StringCodec
import org.redisson.codec.CompositeCodec
import java.lang.reflect.InvocationTargetException

class RedisHeadMetadataSafetyTest: AbstractJaversTest() {

    @Test
    fun `lettuce restores valid head after restart and rejects corrupt metadata`() {
        val repositoryName = "metadata-safety-lettuce-${Base58.randomString(8)}"
        val corruptCommitId = "999999999999999999999999.0"
        val repository = LettuceCdoSnapshotRepository(repositoryName, lettuceClient)
        val commit = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()
            .commit("author", SnapshotEntity(1))

        LettuceCdoSnapshotRepository(repositoryName, lettuceClient)
            .getHeadId() shouldBeEqualTo commit.id

        lettuceClient.connect(LettuceBinaryCodecs.lz4Fory<Any>()).use { connection ->
            connection.sync().hset(
                "javers:$repositoryName:sequence:set",
                corruptCommitId,
                "${commit.id.majorId + 1}",
            )
        }

        val failure = assertFailsWith<IllegalStateException> {
            LettuceCdoSnapshotRepository(repositoryName, lettuceClient).getHeadId()
        }
        failure.message shouldContain "type=commitId"
        failure.message.shouldNotContain(corruptCommitId)
    }

    @Test
    fun `lettuce rejects corrupt sequence metadata after restart`() {
        val repositoryName = "metadata-sequence-safety-lettuce-${Base58.randomString(8)}"
        val repository = LettuceCdoSnapshotRepository(repositoryName, lettuceClient)
        val commit = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()
            .commit("author", SnapshotEntity(3))

        lettuceClient.connect(LettuceBinaryCodecs.lz4Fory<Any>()).use { connection ->
            connection.sync().hset(
                "javers:$repositoryName:sequence:set",
                commit.id.value(),
                "not-a-sequence",
            )
        }

        val failure = assertFailsWith<IllegalStateException> {
            LettuceCdoSnapshotRepository(repositoryName, lettuceClient).getHeadId()
        }
        failure.message shouldContain "type=sequence"
        failure.message.shouldNotContain("not-a-sequence")

        val sequenceFailure = assertFailsWith<IllegalStateException> {
            LettuceCdoSnapshotRepository(repositoryName, lettuceClient).sequenceOf(commit.id)
        }
        sequenceFailure.message shouldContain "type=sequence"
        sequenceFailure.message.shouldNotContain("not-a-sequence")
    }

    @Test
    fun `redisson restores valid head after restart and rejects corrupt metadata`() {
        val repositoryName = "metadata-safety-redisson-${Base58.randomString(8)}"
        val corruptCommitId = "999999999999999999999999.0"
        val repository = RedissonCdoSnapshotRepository(repositoryName, redisson)
        val commit = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()
            .commit("author", SnapshotEntity(2))

        RedissonCdoSnapshotRepository(repositoryName, redisson)
            .getHeadId() shouldBeEqualTo commit.id

        redisson.getMap<String, Long>(
            "javers:$repositoryName:sequence",
            CompositeCodec(StringCodec.INSTANCE, LongCodec.INSTANCE),
        ).fastPut(corruptCommitId, commit.id.majorId + 1)

        val failure = assertFailsWith<IllegalStateException> {
            RedissonCdoSnapshotRepository(repositoryName, redisson).getHeadId()
        }
        failure.message shouldContain "type=commitId"
        failure.message.shouldNotContain(corruptCommitId)
    }

    private fun LettuceCdoSnapshotRepository.sequenceOf(commitId: CommitId): Long {
        val method = LettuceCdoSnapshotRepository::class.java.getDeclaredMethod(
            "getSeq",
            CommitId::class.java,
        )
        method.isAccessible = true
        return try {
            method.invoke(this, commitId) as Long
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }
}
