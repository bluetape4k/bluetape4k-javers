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
 * - projection 반영이 모두 성공한 batch만 [KafkaConsumer.commitSync]로 명시적으로
 *   commit합니다. 하나라도 실패하면 offset을 commit하지 않고 예외를 호출자에게
 *   전파하므로 재시작 후 같은 batch가 다시 전달될 수 있습니다(at-least-once).
 * - 이 consumer가 주입받은 [KafkaConsumer]의 lifecycle을 소유합니다. caller는
 *   [use] 또는 [close]로 consumer를 닫아야 하며, [RedisOrderSummaryProjection]과
 *   그 backing Redis client의 lifecycle은 caller가 별도로 소유합니다.
 */
class OrderProjectionEventConsumer(
    private val consumer: KafkaConsumer<String, String>,
    private val projection: RedisOrderSummaryProjection,
    private val codec: OrderDomainEventJsonCodec = OrderDomainEventJsonCodec(),
): AutoCloseable {

    fun subscribe(topic: String) {
        consumer.subscribe(listOf(topic))
    }

    fun pollOnce(timeout: Duration): Int {
        val records = consumer.poll(timeout)
        records.forEach(::handle)
        if (!records.isEmpty) {
            consumer.commitSync()
        }
        return records.count()
    }

    fun handle(record: ConsumerRecord<String, String>) {
        projection.apply(codec.decode(record.value()))
    }

    override fun close() {
        consumer.close()
    }
}
