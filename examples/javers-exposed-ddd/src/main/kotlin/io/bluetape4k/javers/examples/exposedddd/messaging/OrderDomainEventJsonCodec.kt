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
 * Example-scoped JSON codec for order domain events carried by Kafka.
 *
 * ## Contract
 * The JSON shape is intentionally local to `javers-exposed-ddd`. It supports
 * only the order event types required by the example and fails fast for
 * unsupported events.
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
