package io.bluetape4k.javers.examples.exposedddd.messaging

import io.bluetape4k.javers.ddd.DomainEvent
import io.bluetape4k.javers.ddd.DomainEventPublisher
import io.bluetape4k.javers.examples.exposedddd.domain.OrderId
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.concurrent.TimeUnit

/**
 * Publishes order domain events to Kafka for the query-side projection.
 *
 * ## Contract
 * This example publisher is synchronous and fail-fast. A failed Kafka send
 * fails the command handler through [DomainEventPublisher].
 */
class OrderKafkaEventPublisher(
    private val producer: Producer<String, String>,
    private val topic: String,
    private val codec: OrderDomainEventJsonCodec = OrderDomainEventJsonCodec(),
): DomainEventPublisher {

    override fun publish(event: DomainEvent) {
        val key = (event.aggregateId as OrderId).value
        producer.send(ProducerRecord(topic, key, codec.encode(event))).get(30, TimeUnit.SECONDS)
    }
}
