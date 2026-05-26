package io.bluetape4k.javers.examples.exposedddd.projection

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.bluetape4k.javers.ddd.DomainEvent
import io.bluetape4k.javers.examples.exposedddd.domain.CustomerId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderMarkedPaid
import io.bluetape4k.javers.examples.exposedddd.domain.OrderPlaced
import io.bluetape4k.javers.examples.exposedddd.domain.OrderStatus
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.RedisClient
import io.lettuce.core.api.sync.RedisCommands
import java.math.BigDecimal
import java.time.Instant

/**
 * Stores order summaries in Redis as one JSON document per order.
 *
 * ## Contract
 * The projection is updated from ordered order domain events. It stores the
 * latest query-side summary under a deterministic key derived from [OrderId].
 */
class RedisOrderSummaryProjection(
    private val commands: RedisCommands<String, String>,
    private val keyPrefix: String = "javers-exposed-ddd:order-summary",
) {

    constructor(
        client: RedisClient,
        keyPrefix: String = "javers-exposed-ddd:order-summary",
    ): this(LettuceClients.commands(client), keyPrefix)

    fun apply(event: DomainEvent) {
        when (event) {
            is OrderPlaced -> save(
                OrderSummary(
                    orderId = event.aggregateId,
                    customerId = event.customerId,
                    totalAmount = BigDecimal(event.totalAmount),
                    status = OrderStatus.PLACED,
                    lastUpdated = event.occurredOn,
                ),
            )

            is OrderMarkedPaid -> {
                val current = requireNotNull(findById(event.aggregateId)) {
                    "Order summary not found: ${event.aggregateId.value}"
                }
                save(current.copy(status = OrderStatus.PAID, lastUpdated = event.occurredOn))
            }
        }
    }

    fun save(summary: OrderSummary): OrderSummary {
        commands.set(key(summary.orderId), encode(summary))
        return summary
    }

    fun findById(orderId: OrderId): OrderSummary? {
        return commands.get(key(orderId))?.let(::decode)
    }

    private fun key(orderId: OrderId): String = "$keyPrefix:${orderId.value}"

    private fun encode(summary: OrderSummary): String {
        return JsonObject().apply {
            addProperty("orderId", summary.orderId.value)
            addProperty("customerId", summary.customerId.value)
            addProperty("totalAmount", summary.totalAmount.toPlainString())
            addProperty("status", summary.status.name)
            addProperty("lastUpdated", summary.lastUpdated.toString())
        }.toString()
    }

    private fun decode(json: String): OrderSummary {
        val node = JsonParser.parseString(json).asJsonObject
        return OrderSummary(
            orderId = OrderId(node.get("orderId").asString),
            customerId = CustomerId(node.get("customerId").asString),
            totalAmount = BigDecimal(node.get("totalAmount").asString),
            status = OrderStatus.valueOf(node.get("status").asString),
            lastUpdated = Instant.parse(node.get("lastUpdated").asString),
        )
    }
}
