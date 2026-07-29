package io.bluetape4k.javers.examples.exposedddd.messaging

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.bluetape4k.javers.ddd.DomainEvent
import io.bluetape4k.javers.examples.exposedddd.domain.CustomerId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderMarkedPaid
import io.bluetape4k.javers.examples.exposedddd.domain.OrderPlaced
import java.time.Instant

/**
 * Kafka로 전달되는 주문 domain event를 위한 예제 범위 JSON codec입니다.
 *
 * ## 계약
 * JSON 구조는 의도적으로 `javers-exposed-ddd` 내부에 한정됩니다. 예제에 필요한
 * 주문 event 유형만 지원하며, 지원하지 않는 event는 fail-fast로 처리합니다.
 */
class OrderDomainEventJsonCodec {

    fun encode(event: DomainEvent): String {
        return when (event) {
            is OrderPlaced -> base(event, ORDER_PLACED).apply {
                addProperty("customerId", event.customerId.value)
                addProperty("totalAmount", event.totalAmount)
            }

            is OrderMarkedPaid -> base(event, ORDER_MARKED_PAID)

            else -> error("Unsupported order event type: ${event::class.qualifiedName}")
        }.toString()
    }

    fun decode(json: String): DomainEvent {
        val node = JsonParser.parseString(json).asJsonObject
        val orderId = OrderId(node.get("orderId").asString)
        val occurredOn = Instant.parse(node.get("occurredOn").asString)
        return when (node.get("type").asString) {
            ORDER_PLACED -> OrderPlaced(
                aggregateId = orderId,
                occurredOn = occurredOn,
                customerId = CustomerId(node.get("customerId").asString),
                totalAmount = node.get("totalAmount").asString,
            )

            ORDER_MARKED_PAID -> OrderMarkedPaid(
                aggregateId = orderId,
                occurredOn = occurredOn,
            )

            else -> error("Unsupported order event type: ${node.get("type").asString}")
        }
    }

    private fun base(event: DomainEvent, type: String): JsonObject {
        return JsonObject().apply {
            addProperty("type", type)
            addProperty("orderId", (event.aggregateId as OrderId).value)
            addProperty("occurredOn", event.occurredOn.toString())
        }
    }

    companion object {
        private const val ORDER_PLACED = "OrderPlaced"
        private const val ORDER_MARKED_PAID = "OrderMarkedPaid"
    }
}
