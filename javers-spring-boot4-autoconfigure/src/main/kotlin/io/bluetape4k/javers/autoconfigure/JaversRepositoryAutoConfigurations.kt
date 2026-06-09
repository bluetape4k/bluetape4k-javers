package io.bluetape4k.javers.autoconfigure

import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepository
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepositoryOptions
import io.bluetape4k.javers.persistence.exposed.schema.ExposedJaversTableNames
import io.bluetape4k.javers.persistence.kafka.repository.KafkaCdoSnapshotRepository
import io.bluetape4k.javers.persistence.kafka.repository.VanillaKafkaCdoSnapshotRepository
import io.bluetape4k.javers.persistence.kafka.repository.VanillaKafkaCdoSnapshotRepositoryOptions
import io.bluetape4k.javers.persistence.redis.repository.LettuceCdoSnapshotRepository
import io.bluetape4k.javers.persistence.redis.repository.RedissonCdoSnapshotRepository
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import io.lettuce.core.RedisClient
import org.apache.kafka.clients.producer.Producer
import org.javers.repository.api.JaversRepository
import org.jetbrains.exposed.v1.jdbc.Database
import org.redisson.api.RedissonClient
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration

/**
 * Auto-configures an Exposed JDBC JaVers repository from an existing [Database] bean.
 */
@AutoConfiguration(before = [JaversAutoConfiguration::class])
@ConditionalOnClass(
    name = [
        "io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepository",
        "org.jetbrains.exposed.v1.jdbc.Database",
    ]
)
@ConditionalOnProperty(
    prefix = JaversAutoConfigurationProperties.PREFIX,
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@ConditionalOnProperty(
    prefix = "$JAVERS_PREFIX.repository",
    name = ["type"],
    havingValue = "exposed",
)
@ConditionalOnBean(type = ["org.jetbrains.exposed.v1.jdbc.Database"])
@ConditionalOnMissingBean(type = ["org.javers.repository.api.JaversRepository"])
@EnableConfigurationProperties(JaversAutoConfigurationProperties::class)
class JaversExposedRepositoryAutoConfiguration {

    /**
     * Creates the Exposed-backed JaVers repository.
     */
    @Bean("javersExposedCdoSnapshotRepository")
    fun javersExposedCdoSnapshotRepository(
        database: Database,
        properties: JaversAutoConfigurationProperties,
    ): JaversRepository {
        val exposed = properties.exposed
        val repository = ExposedCdoSnapshotRepository(
            database = database,
            codec = JaversCodecs.String,
            options = ExposedCdoSnapshotRepositoryOptions(
                tableNames = ExposedJaversTableNames(
                    commitTableName = exposed.commitTableName,
                    snapshotTableName = exposed.snapshotTableName,
                ),
                createSchemaOnEnsure = exposed.createSchemaOnEnsure,
            ),
        )

        if (exposed.initializeSchema) {
            repository.ensureSchema()
        }
        return repository
    }
}

/**
 * Auto-configures a Lettuce Redis JaVers repository from an existing [RedisClient] bean.
 */
@AutoConfiguration(before = [JaversAutoConfiguration::class])
@ConditionalOnClass(
    name = [
        "io.bluetape4k.javers.persistence.redis.repository.LettuceCdoSnapshotRepository",
        "io.lettuce.core.RedisClient",
        "net.jpountz.lz4.LZ4Factory",
        "org.apache.fory.ThreadSafeFory",
    ]
)
@ConditionalOnProperty(
    prefix = JaversAutoConfigurationProperties.PREFIX,
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@ConditionalOnProperty(
    prefix = "$JAVERS_PREFIX.repository",
    name = ["type"],
    havingValue = "lettuce",
)
@ConditionalOnBean(type = ["io.lettuce.core.RedisClient"])
@ConditionalOnMissingBean(type = ["org.javers.repository.api.JaversRepository"])
@EnableConfigurationProperties(JaversAutoConfigurationProperties::class)
class JaversLettuceRepositoryAutoConfiguration {

    /**
     * Creates the Lettuce-backed JaVers repository.
     */
    @Bean("javersLettuceCdoSnapshotRepository")
    fun javersLettuceCdoSnapshotRepository(
        redisClient: RedisClient,
        properties: JaversAutoConfigurationProperties,
    ): JaversRepository =
        LettuceCdoSnapshotRepository(
            name = properties.redis.name.validatedRedisName(),
            client = redisClient,
            codec = properties.redis.codec.toByteArrayCodec(),
        )
}

/**
 * Auto-configures a Redisson Redis JaVers repository from an existing [RedissonClient] bean.
 */
@AutoConfiguration(before = [JaversAutoConfiguration::class])
@ConditionalOnClass(
    name = [
        "io.bluetape4k.javers.persistence.redis.repository.RedissonCdoSnapshotRepository",
        "org.redisson.api.RedissonClient",
        "net.jpountz.lz4.LZ4Factory",
        "org.apache.fory.ThreadSafeFory",
    ]
)
@ConditionalOnProperty(
    prefix = JaversAutoConfigurationProperties.PREFIX,
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@ConditionalOnProperty(
    prefix = "$JAVERS_PREFIX.repository",
    name = ["type"],
    havingValue = "redisson",
)
@ConditionalOnBean(type = ["org.redisson.api.RedissonClient"])
@ConditionalOnMissingBean(type = ["org.javers.repository.api.JaversRepository"])
@EnableConfigurationProperties(JaversAutoConfigurationProperties::class)
class JaversRedissonRepositoryAutoConfiguration {

    /**
     * Creates the Redisson-backed JaVers repository.
     */
    @Bean("javersRedissonCdoSnapshotRepository")
    fun javersRedissonCdoSnapshotRepository(
        redissonClient: RedissonClient,
        properties: JaversAutoConfigurationProperties,
    ): JaversRepository =
        RedissonCdoSnapshotRepository(
            name = properties.redis.name.validatedRedisName(),
            redisson = redissonClient,
            codec = properties.redis.codec.toByteArrayCodec(),
        )
}

/**
 * Auto-configures a Spring Kafka write-only JaVers repository from an existing [KafkaTemplate] bean.
 */
@AutoConfiguration(before = [JaversAutoConfiguration::class])
@ConditionalOnClass(
    name = [
        "io.bluetape4k.javers.persistence.kafka.repository.KafkaCdoSnapshotRepository",
        "org.springframework.kafka.core.KafkaTemplate",
    ]
)
@ConditionalOnProperty(
    prefix = JaversAutoConfigurationProperties.PREFIX,
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@ConditionalOnProperty(
    prefix = "$JAVERS_PREFIX.repository",
    name = ["type"],
    havingValue = "spring-kafka",
)
@ConditionalOnBean(type = ["org.springframework.kafka.core.KafkaTemplate"])
@ConditionalOnMissingBean(type = ["org.javers.repository.api.JaversRepository"])
@EnableConfigurationProperties(JaversAutoConfigurationProperties::class)
class JaversSpringKafkaRepositoryAutoConfiguration {

    /**
     * Creates the Spring Kafka write-only JaVers repository.
     */
    @Bean("javersSpringKafkaCdoSnapshotRepository")
    fun javersSpringKafkaCdoSnapshotRepository(
        kafkaTemplate: KafkaTemplate<String, String>,
        properties: JaversAutoConfigurationProperties,
    ): JaversRepository =
        KafkaCdoSnapshotRepository(
            kafkaOperations = kafkaTemplate,
            publishTimeout = properties.kafka.publishTimeout.validatedPublishTimeout(),
        )
}

/**
 * Auto-configures a vanilla Kafka write-only JaVers repository from an existing [Producer] bean.
 */
@AutoConfiguration(before = [JaversAutoConfiguration::class])
@ConditionalOnClass(
    name = [
        "io.bluetape4k.javers.persistence.kafka.repository.VanillaKafkaCdoSnapshotRepository",
        "org.apache.kafka.clients.producer.Producer",
    ]
)
@ConditionalOnProperty(
    prefix = JaversAutoConfigurationProperties.PREFIX,
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@ConditionalOnProperty(
    prefix = "$JAVERS_PREFIX.repository",
    name = ["type"],
    havingValue = "vanilla-kafka",
)
@ConditionalOnBean(type = ["org.apache.kafka.clients.producer.Producer"])
@ConditionalOnMissingBean(type = ["org.javers.repository.api.JaversRepository"])
@EnableConfigurationProperties(JaversAutoConfigurationProperties::class)
class JaversVanillaKafkaRepositoryAutoConfiguration {

    /**
     * Creates the vanilla Kafka write-only JaVers repository.
     */
    @Bean("javersVanillaKafkaCdoSnapshotRepository")
    fun javersVanillaKafkaCdoSnapshotRepository(
        producer: Producer<String, String>,
        properties: JaversAutoConfigurationProperties,
    ): JaversRepository =
        VanillaKafkaCdoSnapshotRepository(
            producer = producer,
            options = VanillaKafkaCdoSnapshotRepositoryOptions(
                topic = properties.kafka.topic,
                publishTimeout = properties.kafka.publishTimeout.validatedPublishTimeout(),
                flushAfterSend = properties.kafka.flushAfterSend,
                closeProducerOnClose = properties.kafka.closeProducerOnClose,
            ),
        )
}

private const val JAVERS_PREFIX = JaversAutoConfigurationProperties.PREFIX

private fun JaversRedisCodec.toByteArrayCodec(): JaversCodec<ByteArray> =
    when (this) {
        JaversRedisCodec.LZ4_FORY -> JaversCodecs.LZ4Fory
    }

private fun String.validatedRedisName(): String =
    requireNotBlank("redis.name")

private fun Duration.validatedPublishTimeout(): Duration =
    apply { toMillis().requirePositiveNumber("kafka.publishTimeout") }
