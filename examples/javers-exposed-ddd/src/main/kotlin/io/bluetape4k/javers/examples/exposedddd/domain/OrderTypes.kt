package io.bluetape4k.javers.examples.exposedddd.domain

import java.io.Serializable
import java.math.BigDecimal

/**
 * 주문 aggregate의 안정적인 식별자입니다.
 */
data class OrderId(
    val value: String,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * 예제 domain에서 고객을 식별하는 안정적인 식별자입니다.
 */
data class CustomerId(
    val value: String,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * command-side 주문 aggregate 안에 저장되는 단일 주문 line입니다.
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
 * command-side 주문 aggregate의 생명주기 상태입니다.
 */
enum class OrderStatus {
    PLACED,
    PAID,
}
