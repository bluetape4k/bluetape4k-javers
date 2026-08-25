package io.bluetape4k.javers.autoconfigure

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.codec.Base58
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepository
import io.bluetape4k.javers.persistence.exposed.schema.CommitTable
import io.bluetape4k.javers.persistence.kafka.repository.KafkaCdoSnapshotRepository
import io.bluetape4k.javers.persistence.kafka.repository.VanillaKafkaCdoSnapshotRepository
import io.bluetape4k.javers.persistence.redis.repository.LettuceCdoSnapshotRepository
import io.bluetape4k.javers.persistence.redis.repository.RedissonCdoSnapshotRepository
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.RedisCodec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.producer.Producer
import org.javers.core.Javers
import org.javers.repository.api.JaversRepository
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.redisson.api.RListMultimap
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import org.redisson.client.codec.Codec
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.io.Serializable
import java.util.concurrent.CompletableFuture

class JaversAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                JaversExposedRepositoryAutoConfiguration::class.java,
                JaversLettuceRepositoryAutoConfiguration::class.java,
                JaversRedissonRepositoryAutoConfiguration::class.java,
                JaversSpringKafkaRepositoryAutoConfiguration::class.java,
                JaversVanillaKafkaRepositoryAutoConfiguration::class.java,
                JaversAutoConfiguration::class.java,
            )
        )

    @Test
    fun `AutoConfiguration imports list every phase directly`() {
        val imports = javaClass.classLoader
            .getResource("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
            ?.readText()
            ?.lineSequence()
            ?.filter { it.isNotBlank() && !it.startsWith("#") }
            ?.toList()

        imports shouldBeEqualTo listOf(
            JaversExposedRepositoryAutoConfiguration::class.qualifiedName,
            JaversLettuceRepositoryAutoConfiguration::class.qualifiedName,
            JaversRedissonRepositoryAutoConfiguration::class.qualifiedName,
            JaversSpringKafkaRepositoryAutoConfiguration::class.qualifiedName,
            JaversVanillaKafkaRepositoryAutoConfiguration::class.qualifiedName,
            JaversAutoConfiguration::class.qualifiedName,
        )
    }

    @Test
    fun `Spring Kafka repository phase is ordered after Spring Boot Kafka auto configuration`() {
        val annotation = JaversSpringKafkaRepositoryAutoConfiguration::class.java
            .getAnnotation(org.springframework.boot.autoconfigure.AutoConfiguration::class.java)

        annotation.afterName.toList() shouldBeEqualTo listOf(KafkaAutoConfiguration::class.qualifiedName)
    }

    @Test
    fun `Exposed schema initialization defaults are migration owned`() {
        contextRunner
            .withPropertyValues("bluetape4k.javers.repository.type=exposed")
            .withUserConfiguration(ExposedDatabaseConfiguration::class.java)
            .run { context ->
                val properties = context.getBean(JaversAutoConfigurationProperties::class.java)

                properties.exposed.initializeSchema.shouldBeFalse()
                properties.exposed.createSchemaOnEnsure.shouldBeFalse()
            }
    }

    @Test
    fun `external migration ownership leaves tables untouched`() {
        val database = newDatabase()

        contextRunner
            .withPropertyValues("bluetape4k.javers.repository.type=exposed")
            .withBean(Database::class.java, { database })
            .run { context ->
                context.getBean("javersExposedCdoSnapshotRepository", ExposedCdoSnapshotRepository::class.java)
                    .ensureSchema()

                assertFailsWith<Exception> {
                    transaction(database) { CommitTable.selectAll().count() }
                }
            }
    }

    @Test
    fun `initialize schema creates tables while repository bean starts`() {
        val database = newDatabase()

        contextRunner
            .withPropertyValues(
                "bluetape4k.javers.repository.type=exposed",
                "bluetape4k.javers.exposed.initialize-schema=true",
                "bluetape4k.javers.exposed.create-schema-on-ensure=true",
            )
            .withBean(Database::class.java, { database })
            .run { context ->
                context.getBean("javersExposedCdoSnapshotRepository", ExposedCdoSnapshotRepository::class.java)
                transaction(database) { CommitTable.selectAll().count() shouldBeEqualTo 0L }
            }
    }

    @Test
    fun `initialize schema without creation fails fast`() {
        val database = newDatabase()

        contextRunner
            .withPropertyValues(
                "bluetape4k.javers.repository.type=exposed",
                "bluetape4k.javers.exposed.initialize-schema=true",
                "bluetape4k.javers.exposed.create-schema-on-ensure=false",
            )
            .withBean(Database::class.java, { database })
            .run { context ->
                val failure = context.startupFailure.shouldNotBeNull()
                failure.toString() shouldContain "initialize-schema=true"
            }
    }

    @Test
    fun `does not register repository when repository type is none`() {
        contextRunner
            .withUserConfiguration(ExposedDatabaseConfiguration::class.java)
            .run { context ->
                context.getBeansOfType(JaversRepository::class.java) shouldHaveSize 0
                context.containsBean("javers").shouldBeFalse()
            }
    }

    @Test
    fun `does not register repository when globally disabled`() {
        contextRunner
            .withPropertyValues(
                "bluetape4k.javers.enabled=false",
                "bluetape4k.javers.repository.type=exposed",
            )
            .withUserConfiguration(ExposedDatabaseConfiguration::class.java)
            .run { context ->
                context.getBeansOfType(JaversRepository::class.java) shouldHaveSize 0
                context.containsBean("javers").shouldBeFalse()
            }
    }

    @Test
    fun `registers Exposed repository and default Javers`() {
        contextRunner
            .withPropertyValues("bluetape4k.javers.repository.type=exposed")
            .withUserConfiguration(ExposedDatabaseConfiguration::class.java)
            .run { context ->
                context.containsBean("javersExposedCdoSnapshotRepository").shouldBeTrue()
                context.getBean(JaversRepository::class.java) shouldBeInstanceOf ExposedCdoSnapshotRepository::class
                context.containsBean("javers").shouldBeTrue()
            }
    }

    @Test
    fun `registers Lettuce repository from RedisClient bean`() {
        contextRunner
            .withPropertyValues("bluetape4k.javers.repository.type=lettuce")
            .withUserConfiguration(LettuceConfiguration::class.java)
            .run { context ->
                context.containsBean("javersLettuceCdoSnapshotRepository").shouldBeTrue()
                context.getBean(JaversRepository::class.java) shouldBeInstanceOf LettuceCdoSnapshotRepository::class
            }
    }

    @Test
    fun `Spring context closes Lettuce repository connections but leaves RedisClient caller owned`() {
        val client = mockk<RedisClient>(relaxed = true)
        val connection = mockk<StatefulRedisConnection<String, Any>>(relaxed = true)
        val commands = mockk<RedisCommands<String, Any>>(relaxed = true)

        every { client.connect(any<RedisCodec<String, Any>>()) } returns connection
        every { connection.sync() } returns commands
        every { commands.hgetall(any()) } returns emptyMap()

        contextRunner
            .withPropertyValues("bluetape4k.javers.repository.type=lettuce")
            .withBean(RedisClient::class.java, { client })
            .run { context ->
                val repository = context.getBean(
                    "javersLettuceCdoSnapshotRepository",
                    LettuceCdoSnapshotRepository::class.java,
                )

                repository.getHeadId()
                verify(exactly = 0) { connection.close() }
            }

        verify(exactly = 1) { connection.close() }
        verify(exactly = 0) { client.shutdown() }
    }

    @Test
    fun `registers Redisson repository from RedissonClient bean`() {
        contextRunner
            .withPropertyValues("bluetape4k.javers.repository.type=redisson")
            .withUserConfiguration(RedissonConfiguration::class.java)
            .run { context ->
                context.containsBean("javersRedissonCdoSnapshotRepository").shouldBeTrue()
                context.getBean(JaversRepository::class.java) shouldBeInstanceOf RedissonCdoSnapshotRepository::class
            }
    }

    @Test
    fun `registers Spring Kafka repository from KafkaTemplate bean`() {
        contextRunner
            .withPropertyValues("bluetape4k.javers.repository.type=spring-kafka")
            .withUserConfiguration(SpringKafkaConfiguration::class.java)
            .run { context ->
                context.containsBean("javersSpringKafkaCdoSnapshotRepository").shouldBeTrue()
                context.getBean(JaversRepository::class.java) shouldBeInstanceOf KafkaCdoSnapshotRepository::class
            }
    }

    @Test
    fun `registers Spring Kafka repository after Boot creates KafkaTemplate`() {
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    KafkaAutoConfiguration::class.java,
                    JaversSpringKafkaRepositoryAutoConfiguration::class.java,
                    JaversAutoConfiguration::class.java,
                )
            )
            .withPropertyValues(
                "spring.kafka.bootstrap-servers=localhost:9092",
                "bluetape4k.javers.repository.type=spring-kafka",
            )
            .run { context ->
                context.containsBean("kafkaTemplate").shouldBeTrue()
                context.containsBean("javersSpringKafkaCdoSnapshotRepository").shouldBeTrue()
                context.getBean(JaversRepository::class.java) shouldBeInstanceOf KafkaCdoSnapshotRepository::class
            }
    }

    @Test
    fun `Spring Kafka repository publishes to configured topic`() {
        contextRunner
            .withPropertyValues(
                "bluetape4k.javers.repository.type=spring-kafka",
                "bluetape4k.javers.kafka.topic=audit-topic",
            )
            .withUserConfiguration(SpringKafkaConfiguration::class.java)
            .run { context ->
                val javers = context.getBean(Javers::class.java)
                @Suppress("UNCHECKED_CAST")
                val kafkaTemplate = context.getBean(KafkaTemplate::class.java) as KafkaTemplate<String, String>

                javers.commit("author", AuditEntity(1L, "created"))

                verify { kafkaTemplate.send("audit-topic", any<String>(), any<String>()) }
                verify(exactly = 0) { kafkaTemplate.sendDefault(any<String>(), any<String>()) }
            }
    }

    @Test
    fun `registers vanilla Kafka repository from Producer bean`() {
        contextRunner
            .withPropertyValues(
                "bluetape4k.javers.repository.type=vanilla-kafka",
                "bluetape4k.javers.kafka.topic=audit-snapshots",
            )
            .withUserConfiguration(VanillaKafkaConfiguration::class.java)
            .run { context ->
                context.containsBean("javersVanillaKafkaCdoSnapshotRepository").shouldBeTrue()
                context.getBean(JaversRepository::class.java) shouldBeInstanceOf VanillaKafkaCdoSnapshotRepository::class
            }
    }

    @Test
    fun `backs off when user provides JaversRepository`() {
        contextRunner
            .withPropertyValues("bluetape4k.javers.repository.type=exposed")
            .withUserConfiguration(ExposedDatabaseConfiguration::class.java, UserRepositoryConfiguration::class.java)
            .run { context ->
                context.getBeansOfType(JaversRepository::class.java).keys shouldBeEqualTo setOf("userJaversRepository")
                context.containsBean("javersExposedCdoSnapshotRepository").shouldBeFalse()
            }
    }

    @Test
    fun `backs off when user provides Javers`() {
        contextRunner
            .withPropertyValues("bluetape4k.javers.repository.type=exposed")
            .withUserConfiguration(ExposedDatabaseConfiguration::class.java, UserJaversConfiguration::class.java)
            .run { context ->
                context.containsBean("javersExposedCdoSnapshotRepository").shouldBeTrue()
                context.getBeansOfType(Javers::class.java).keys shouldBeEqualTo setOf("userJavers")
            }
    }

    @Test
    fun `does not fail when selected backend class is missing`() {
        contextRunner
            .withClassLoader(FilteredClassLoader(ExposedCdoSnapshotRepository::class.java))
            .withPropertyValues("bluetape4k.javers.repository.type=exposed")
            .withUserConfiguration(ExposedDatabaseConfiguration::class.java)
            .run { context ->
                context.getBeansOfType(JaversRepository::class.java) shouldHaveSize 0
                context.containsBean("javers").shouldBeFalse()
            }
    }

    @Test
    fun `does not fail when Spring Kafka class is missing`() {
        contextRunner
            .withClassLoader(FilteredClassLoader(KafkaTemplate::class.java))
            .withPropertyValues("bluetape4k.javers.repository.type=spring-kafka")
            .withUserConfiguration(SpringKafkaConfiguration::class.java)
            .run { context ->
                context.getBeansOfType(JaversRepository::class.java) shouldHaveSize 0
                context.containsBean("javers").shouldBeFalse()
            }
    }

    @Test
    fun `does not fail when Redis codec class is missing`() {
        contextRunner
            .withClassLoader(FilteredClassLoader("org.apache.fory"))
            .withPropertyValues("bluetape4k.javers.repository.type=lettuce")
            .withUserConfiguration(LettuceConfiguration::class.java)
            .run { context ->
                context.getBeansOfType(JaversRepository::class.java) shouldHaveSize 0
                context.containsBean("javers").shouldBeFalse()
            }
    }

    @TestConfiguration(proxyBeanMethods = false)
    class ExposedDatabaseConfiguration {

        @Bean
        fun exposedDatabase(): Database =
            Database.connect(
                url = "jdbc:h2:mem:javers-auto-config-${Base58.randomString(8)};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
            )
    }

    @TestConfiguration(proxyBeanMethods = false)
    class LettuceConfiguration {

        @Bean
        fun redisClient(): RedisClient = mockk(relaxed = true)
    }

    @TestConfiguration(proxyBeanMethods = false)
    class RedissonConfiguration {

        @Bean
        fun redissonClient(): RedissonClient {
            val redisson = mockk<RedissonClient>()
            val snapshots = mockk<RListMultimap<String, ByteArray>>(relaxed = true)
            val sequence = mockk<RMap<String, Long>>(relaxed = true)

            every { redisson.getListMultimap<String, ByteArray>(any(), any<Codec>()) } returns snapshots
            every { redisson.getMap<String, Long>(any(), any<Codec>()) } returns sequence

            return redisson
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    class SpringKafkaConfiguration {

        @Bean
        fun kafkaTemplate(): KafkaTemplate<String, String> {
            val kafkaTemplate = mockk<KafkaTemplate<String, String>>(relaxed = true)
            val successfulSend = CompletableFuture.completedFuture(mockk<SendResult<String, String>>(relaxed = true))
            val failedDefaultSend = CompletableFuture.failedFuture<SendResult<String, String>>(
                AssertionError("configured topic should be used")
            )

            every { kafkaTemplate.send(any<String>(), any<String>(), any<String>()) } returns successfulSend
            every { kafkaTemplate.sendDefault(any<String>(), any<String>()) } returns failedDefaultSend

            return kafkaTemplate
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    class VanillaKafkaConfiguration {

        @Bean
        fun kafkaProducer(): Producer<String, String> = mockk(relaxed = true)
    }

    @TestConfiguration(proxyBeanMethods = false)
    class UserRepositoryConfiguration {

        @Bean
        fun userJaversRepository(): JaversRepository = mockk(relaxed = true)
    }

    @TestConfiguration(proxyBeanMethods = false)
    class UserJaversConfiguration {

        @Bean
        fun userJavers(): Javers = mockk(relaxed = true)
    }

    data class AuditEntity(
        val id: Long,
        val status: String,
    ): Serializable {

        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    private fun newDatabase(): Database = Database.connect(
        url = "jdbc:h2:mem:javers-auto-config-schema-${Base58.randomString(8)};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
    )
}
