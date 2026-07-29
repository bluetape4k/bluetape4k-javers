package io.bluetape4k.javers.autoconfigure

import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.repository.api.JaversRepository
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

/**
 * 기본 [Javers] bean을 구성하는 Spring Boot auto-configuration 기반 단계입니다.
 *
 * ## 계약
 * 이 단계는 repository auto-configuration 단계 이후에 실행됩니다. application이
 * 자체 [Javers] bean을 제공하면 물러나며, 정렬된 [JaversBuilderCustomizer] bean으로
 * 기본 builder를 확장할 수 있게 합니다.
 */
@AutoConfiguration(
    after = [
        JaversExposedRepositoryAutoConfiguration::class,
        JaversLettuceRepositoryAutoConfiguration::class,
        JaversRedissonRepositoryAutoConfiguration::class,
        JaversSpringKafkaRepositoryAutoConfiguration::class,
        JaversVanillaKafkaRepositoryAutoConfiguration::class,
    ]
)
@ConditionalOnClass(
    name = [
        "org.javers.core.Javers",
        "org.javers.core.JaversBuilder",
        "org.javers.repository.api.JaversRepository",
    ]
)
@ConditionalOnProperty(
    prefix = JaversAutoConfigurationProperties.PREFIX,
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@ConditionalOnBean(type = ["org.javers.repository.api.JaversRepository"])
@ConditionalOnMissingBean(type = ["org.javers.core.Javers"])
@EnableConfigurationProperties(JaversAutoConfigurationProperties::class)
class JaversAutoConfiguration {

    /**
     * 선택된 repository가 backing하는 기본 JaVers instance를 생성합니다.
     */
    @Bean
    fun javers(
        repository: JaversRepository,
        customizers: ObjectProvider<JaversBuilderCustomizer>,
    ): Javers {
        val builder = JaversBuilder.javers()
            .registerJaversRepository(repository)

        customizers.orderedStream().forEach { it.customize(builder) }
        return builder.build()
    }
}

/**
 * auto-configured [JaversBuilder]를 application에서 조정할 때 사용하는 callback입니다.
 */
fun interface JaversBuilderCustomizer {

    /**
     * application별 registration을 [builder]에 적용합니다.
     */
    fun customize(builder: JaversBuilder)
}
