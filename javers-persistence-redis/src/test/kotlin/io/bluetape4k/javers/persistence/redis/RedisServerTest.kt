package io.bluetape4k.javers.persistence.redis

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.redisson.api.redisnode.RedisNode
import org.redisson.api.redisnode.RedisNodes

class RedisServerTest: AbstractJaversTest() {

    companion object: KLogging()

    @Test
    fun `Lettuce Client 접속 테스트`() {
        lettuceClient.shouldNotBeNull()

        lettuceClient.connect().use { conn ->
            val commands = conn.sync()
            commands.ping() shouldBeEqualTo "PONG"
            log.debug { "Redis info=${commands.info()}" }
        }
    }

    @Test
    fun `Redisson 으로 접속 테스트`() {
        redisson.shouldNotBeNull()

        val instance = redisson.getRedisNodes(RedisNodes.SINGLE).instance
        instance.ping().shouldBeTrue()
        log.debug { "Redis info=${instance.info(RedisNode.InfoSection.ALL)}" }
    }
}
