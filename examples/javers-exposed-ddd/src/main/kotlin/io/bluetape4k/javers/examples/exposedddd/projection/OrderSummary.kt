package io.bluetape4k.javers.examples.exposedddd.projection

import io.bluetape4k.javers.examples.exposedddd.domain.CustomerId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderStatus
import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant

/**
 * Redis-backed query-side read model for the order CQRS example.
 *
 * ## Contract
 * Instances are derived from domain events and optimized for reads. They are
 * not the command-side source of truth.
 */
data class OrderSummary(
    val orderId: OrderId,
    val customerId: CustomerId,
    val totalAmount: BigDecimal,
    val status: OrderStatus,
    val lastUpdated: Instant,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
