package io.bluetape4k.javers.examples.ktor.domain

import java.io.Serializable
import java.math.BigDecimal

/**
 * Stable identifier for an order aggregate.
 */
data class OrderId(
    val value: String,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Stable identifier for a customer in the example domain.
 */
data class CustomerId(
    val value: String,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Single order line persisted inside the command-side order aggregate.
 */
data class OrderItem(
    val sku: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
): Serializable {

    val lineTotal: BigDecimal
        get() = unitPrice.multiply(quantity.toBigDecimal())

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Lifecycle state of the command-side order aggregate.
 */
enum class OrderStatus {
    PLACED,
    PAID,
}
