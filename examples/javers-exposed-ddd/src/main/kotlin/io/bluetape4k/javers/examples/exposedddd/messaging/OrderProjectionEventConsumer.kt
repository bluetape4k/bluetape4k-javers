package io.bluetape4k.javers.examples.exposedddd.messaging

import io.bluetape4k.javers.examples.exposedddd.projection.RedisOrderSummaryProjection
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

/**
 * Kafka 주문 event를 소비해 Redis read model에 반영합니다.
 *
 * ## 계약
 * record는 poll 순서대로 적용됩니다. Kafka ordering은 주문 key별로 유지된다고
 * 가정하므로, 같은 aggregate의 후속 상태 변경 event보다 `OrderPlaced`가 먼저
 * 도착해야 합니다.
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
