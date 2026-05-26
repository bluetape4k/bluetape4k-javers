package io.bluetape4k.javers.ddd.kafka

import io.bluetape4k.javers.ddd.DomainEvent
import io.bluetape4k.javers.ddd.DomainEventPublisher
import io.bluetape4k.javers.ddd.spring.publishAfterCommit
import org.springframework.kafka.core.KafkaTemplate

/**
 * Publishes domain events to Kafka with Spring Kafka's [KafkaTemplate].
 *
 * ## Contract
 * If Spring transaction synchronization is active, events are sent in
 * `afterCommit`. Otherwise, they are sent immediately.
 *
 * @property kafkaTemplate template used to send domain events
 * @property topicResolver maps a domain event to a Kafka topic
 * @property keyResolver maps a domain event to a Kafka record key
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
