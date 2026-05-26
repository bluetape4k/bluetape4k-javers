package io.bluetape4k.javers.examples.exposedddd.messaging

import io.bluetape4k.javers.examples.exposedddd.projection.RedisOrderSummaryProjection
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

/**
 * Consumes Kafka order events and applies them to the Redis read model.
 *
 * ## Contract
 * Records are applied in poll order. Kafka ordering is expected per order key,
 * so `OrderPlaced` should arrive before later status-change events for the same
 * aggregate.
 */
class OrderProjectionEventConsumer(
    private val consumer: KafkaConsumer<String, String>,
    private val projection: RedisOrderSummaryProjection,
    private val codec: OrderDomainEventJsonCodec = OrderDomainEventJsonCodec(),
) {

    fun subscribe(topic: String) {
        consumer.subscribe(listOf(topic))
    }

    fun pollOnce(timeout: Duration): Int {
        val records = consumer.poll(timeout)
        records.forEach(::handle)
        return records.count()
    }

    fun handle(record: ConsumerRecord<String, String>) {
        projection.apply(codec.decode(record.value()))
    }
}
