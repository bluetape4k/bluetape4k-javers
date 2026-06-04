package io.bluetape4k.javers.examples.ktor.domain

import java.io.Serializable

/**
 * Command accepted by the order Ktor example.
 */
sealed interface OrderCommand {
    val orderId: OrderId
    val author: String
}

/**
 * Places a new order for [customerId].
 */
data class PlaceOrderCommand(
    override val orderId: OrderId,
    override val author: String,
    val customerId: CustomerId,
    val items: List<OrderItem>,
): OrderCommand, Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Marks an existing order as paid.
 */
data class MarkOrderPaidCommand(
    override val orderId: OrderId,
    override val author: String,
): OrderCommand, Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
