package io.bluetape4k.javers.ddd.kafka

import io.bluetape4k.javers.ddd.DomainEvent
import io.bluetape4k.javers.ddd.DomainEventPublisher
import io.bluetape4k.javers.ddd.spring.publishAfterCommit
import org.springframework.kafka.core.KafkaTemplate

/**
 * Spring Kafka [KafkaTemplate]로 domain event를 Kafka에 publish합니다.
 *
 * ## 계약
 * Spring transaction synchronization이 active이면 event를 `afterCommit`에서 보냅니다.
 * 그렇지 않으면 즉시 보냅니다.
 *
 * @property kafkaTemplate domain event 전송에 사용하는 template입니다.
 * @property topicResolver domain event를 Kafka topic으로 mapping합니다.
 * @property keyResolver domain event를 Kafka record key로 mapping합니다.
 */
class KafkaDomainEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, DomainEvent>,
    private val topicResolver: (DomainEvent) -> String,
    private val keyResolver: (DomainEvent) -> String = { it.aggregateId.toString() },
): DomainEventPublisher {

    override fun publish(event: DomainEvent) {
        publishAfterCommit {
            kafkaTemplate.send(topicResolver(event), keyResolver(event), event)
        }
    }
}
