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
 * 주문 summary를 주문별 JSON document 하나로 Redis에 저장합니다.
 *
 * ## 계약
 * 이 projection은 정렬된 주문 domain event로 갱신됩니다. 최신 query-side summary를
 * [OrderId]에서 파생한 결정적 key 아래에 저장합니다.
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
