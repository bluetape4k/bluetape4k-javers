package io.bluetape4k.javers.examples.ktor.domain

import java.io.Serializable

/**
 * 주문 Ktor 예제가 처리하는 command입니다.
 */
sealed interface OrderCommand {
    val orderId: OrderId
    val author: String
}

/**
 * [customerId] 고객의 새 주문을 생성합니다.
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
 * 기존 주문을 결제 완료 상태로 표시합니다.
 */
data class MarkOrderPaidCommand(
    override val orderId: OrderId,
    override val author: String,
): OrderCommand, Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
