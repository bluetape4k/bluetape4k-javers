package io.bluetape4k.javers.examples.springboot4.config

import io.bluetape4k.javers.ddd.DomainEventPublisher
import io.bluetape4k.javers.ddd.NoopDomainEventPublisher
import io.bluetape4k.javers.examples.springboot4.domain.Order
import io.bluetape4k.javers.examples.springboot4.persistence.OrderRepository
import io.bluetape4k.javers.examples.springboot4.persistence.OrdersTable
import io.bluetape4k.javers.examples.springboot4.service.OrderCommandHandler
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepository
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTable
import io.bluetape4k.javers.persistence.exposed.schema.CommitTable
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/**
 * Spring Boot 4 예제를 위한 명시적 runtime wiring입니다.
 *
 * ## 계약
 * 이 configuration은 Exposed database, JaVers repository, 주문 command handler를
 * 직접 생성합니다. 예제 내부 wiring이며 bluetape4k-javers용 Spring Boot
 * auto-configuration이 아닙니다.
 */
@Configuration(proxyBeanMethods = false)
class JaversExampleConfiguration {

    @Bean
    fun exampleDatabase(): Database {
        return Database.connect(
            url = "jdbc:h2:mem:javers-spring-boot4;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
    }

    @Bean
    fun exampleSchemaInitializer(database: Database): InitializingBean {
        return InitializingBean {
            transaction(database) {
                SchemaUtils.create(CommitTable, CdoSnapshotTable, OrdersTable)
            }
        }
    }

    @Bean
    fun exampleJavers(database: Database): Javers {
        return JaversBuilder.javers()
            .registerJaversRepository(ExposedCdoSnapshotRepository(database))
            .registerEntity(Order::class.java)
            .build()
    }

    @Bean
    fun exampleDomainEventPublisher(): DomainEventPublisher = NoopDomainEventPublisher

    @Bean
    fun exampleOrderRepository(
        database: Database,
        javers: Javers,
        eventPublisher: DomainEventPublisher,
    ): OrderRepository {
        return OrderRepository(database, javers, eventPublisher)
    }

    @Bean
    fun exampleClock(): Clock = Clock.systemUTC()

    @Bean
    fun exampleOrderCommandHandler(repository: OrderRepository, clock: Clock): OrderCommandHandler {
        return OrderCommandHandler(repository, clock)
    }
}
