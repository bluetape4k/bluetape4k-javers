package io.bluetape4k.javers.autoconfigure

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.codec.Base58
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepository
import io.bluetape4k.javers.persistence.kafka.repository.KafkaCdoSnapshotRepository
import io.bluetape4k.javers.persistence.kafka.repository.VanillaKafkaCdoSnapshotRepository
import io.bluetape4k.javers.persistence.redis.repository.LettuceCdoSnapshotRepository
import io.bluetape4k.javers.persistence.redis.repository.RedissonCdoSnapshotRepository
import io.lettuce.core.RedisClient
import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.clients.producer.Producer
import org.javers.core.Javers
import org.javers.repository.api.JaversRepository
import org.jetbrains.exposed.v1.jdbc.Database
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
import org.springframework.kafka.core.KafkaTemplate

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
    fun `does not register repository when repository type is none`() {
        contextRunner
            .withUserConfiguration(ExposedDatabaseConfiguration::class.java)
            .run { context ->
                context.getBeansOfType(JaversRepository::class.java).size shouldBeEqualTo 0
                context.containsBean("javers") shouldBeEqualTo false
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
                context.getBeansOfType(JaversRepository::class.java).size shouldBeEqualTo 0
                context.containsBean("javers") shouldBeEqualTo false
            }
    }

    @Test
    fun `registers Exposed repository and default Javers`() {
        contextRunner
            .withPropertyValues("bluetape4k.javers.repository.type=exposed")
            .withUserConfiguration(ExposedDatabaseConfiguration::class.java)
            .run { context ->
                context.containsBean("javersExposedCdoSnapshotRepository") shouldBeEqualTo true
                (context.getBean(JaversRepository::class.java) is ExposedCdoSnapshotRepository) shouldBeEqualTo true
                context.containsBean("javers") shouldBeEqualTo true
            }
    }

    @Test
    fun `registers Lettuce repository from RedisClient bean`() {
        contextRunner
            .withPropertyValues("bluetape4k.javers.repository.type=lettuce")
            .withUserConfiguration(LettuceConfiguration::class.java)
            .run { context ->
                context.containsBean("javersLettuceCdoSnapshotRepository") shouldBeEqualTo true
                (context.getBean(JaversRepository::class.java) is LettuceCdoSnapshotRepository) shouldBeEqualTo true
            }
    }

    @Test
    fun `registers Redisson repository from RedissonClient bean`() {
        contextRunner
            .withPropertyValues("bluetape4k.javers.repository.type=redisson")
            .withUserConfiguration(RedissonConfiguration::class.java)
            .run { context ->
                context.containsBean("javersRedissonCdoSnapshotRepository") shouldBeEqualTo true
                (context.getBean(JaversRepository::class.java) is RedissonCdoSnapshotRepository) shouldBeEqualTo true
            }
    }

    @Test
    fun `registers Spring Kafka repository from KafkaTemplate bean`() {
        contextRunner
            .withPropertyValues("bluetape4k.javers.repository.type=spring-kafka")
            .withUserConfiguration(SpringKafkaConfiguration::class.java)
            .run { context ->
                context.containsBean("javersSpringKafkaCdoSnapshotRepository") shouldBeEqualTo true
                (context.getBean(JaversRepository::class.java) is KafkaCdoSnapshotRepository) shouldBeEqualTo true
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
                context.containsBean("javersVanillaKafkaCdoSnapshotRepository") shouldBeEqualTo true
                (context.getBean(JaversRepository::class.java) is VanillaKafkaCdoSnapshotRepository) shouldBeEqualTo true
            }
    }

    @Test
    fun `backs off when user provides JaversRepository`() {
        contextRunner
            .withPropertyValues("bluetape4k.javers.repository.type=exposed")
            .withUserConfiguration(ExposedDatabaseConfiguration::class.java, UserRepositoryConfiguration::class.java)
            .run { context ->
                context.getBeansOfType(JaversRepository::class.java).keys shouldBeEqualTo setOf("userJaversRepository")
                context.containsBean("javersExposedCdoSnapshotRepository") shouldBeEqualTo false
            }
    }

    @Test
    fun `backs off when user provides Javers`() {
        contextRunner
            .withPropertyValues("bluetape4k.javers.repository.type=exposed")
            .withUserConfiguration(ExposedDatabaseConfiguration::class.java, UserJaversConfiguration::class.java)
            .run { context ->
                context.containsBean("javersExposedCdoSnapshotRepository") shouldBeEqualTo true
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
                context.getBeansOfType(JaversRepository::class.java).size shouldBeEqualTo 0
                context.containsBean("javers") shouldBeEqualTo false
            }
    }

    @Test
    fun `does not fail when Spring Kafka class is missing`() {
        contextRunner
            .withClassLoader(FilteredClassLoader(KafkaTemplate::class.java))
            .withPropertyValues("bluetape4k.javers.repository.type=spring-kafka")
            .withUserConfiguration(SpringKafkaConfiguration::class.java)
            .run { context ->
                context.getBeansOfType(JaversRepository::class.java).size shouldBeEqualTo 0
                context.containsBean("javers") shouldBeEqualTo false
            }
    }

    @Test
    fun `does not fail when Redis codec class is missing`() {
        contextRunner
            .withClassLoader(FilteredClassLoader("org.apache.fory"))
            .withPropertyValues("bluetape4k.javers.repository.type=lettuce")
            .withUserConfiguration(LettuceConfiguration::class.java)
            .run { context ->
                context.getBeansOfType(JaversRepository::class.java).size shouldBeEqualTo 0
                context.containsBean("javers") shouldBeEqualTo false
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
        fun kafkaTemplate(): KafkaTemplate<String, String> = mockk(relaxed = true)
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
}
