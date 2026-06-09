package io.bluetape4k.javers.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import java.io.Serializable
import java.time.Duration

/**
 * Configuration properties for bluetape4k JaVers Spring Boot auto-configuration.
 *
 * ## Contract
 * The auto-configuration is globally enabled by default, but it does not create
 * a repository unless [repository.type] selects a concrete backend. Infrastructure
 * clients such as Exposed `Database`, Redis clients, and Kafka producers remain
 * application-owned beans.
 */
@ConfigurationProperties(prefix = JaversAutoConfigurationProperties.PREFIX)
data class JaversAutoConfigurationProperties(
    val enabled: Boolean = true,
    @field:NestedConfigurationProperty
    val repository: JaversRepositoryProperties = JaversRepositoryProperties(),
    @field:NestedConfigurationProperty
    val exposed: JaversExposedProperties = JaversExposedProperties(),
    @field:NestedConfigurationProperty
    val redis: JaversRedisProperties = JaversRedisProperties(),
    @field:NestedConfigurationProperty
    val kafka: JaversKafkaProperties = JaversKafkaProperties(),
): Serializable {

    companion object {
        const val PREFIX: String = "bluetape4k.javers"
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Backend selection properties for JaVers repository auto-configuration.
 */
data class JaversRepositoryProperties(
    val type: JaversRepositoryType = JaversRepositoryType.NONE,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Repository backend types supported by bluetape4k JaVers auto-configuration.
 */
enum class JaversRepositoryType {
    /** Do not create a repository automatically. */
    NONE,

    /** Create an Exposed JDBC repository from an existing Exposed `Database` bean. */
    EXPOSED,

    /** Create a Lettuce Redis repository from an existing `RedisClient` bean. */
    LETTUCE,

    /** Create a Redisson Redis repository from an existing `RedissonClient` bean. */
    REDISSON,

    /** Create a Spring Kafka write-only repository from an existing `KafkaTemplate` bean. */
    SPRING_KAFKA,

    /** Create a vanilla Kafka write-only repository from an existing Apache Kafka `Producer` bean. */
    VANILLA_KAFKA,
}

/**
 * Exposed JDBC repository properties.
 */
data class JaversExposedProperties(
    val initializeSchema: Boolean = false,
    val createSchemaOnEnsure: Boolean = false,
    val commitTableName: String = "javers_commit",
    val snapshotTableName: String = "javers_snapshot",
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Redis repository properties shared by Lettuce and Redisson backends.
 */
data class JaversRedisProperties(
    val name: String = "default",
    val codec: JaversRedisCodec = JaversRedisCodec.LZ4_FORY,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Safe Redis codec choices exposed by auto-configuration.
 */
enum class JaversRedisCodec {
    /** LZ4-compressed Fory binary codec. */
    LZ4_FORY,
}

/**
 * Kafka repository properties shared by Spring Kafka and vanilla Kafka backends.
 */
data class JaversKafkaProperties(
    val publishTimeout: Duration = Duration.ofSeconds(30),
    val topic: String = "javers-snapshots",
    val flushAfterSend: Boolean = false,
    val closeProducerOnClose: Boolean = false,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
