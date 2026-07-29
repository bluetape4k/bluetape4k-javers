package io.bluetape4k.javers.examples.exposedddd.projection

import io.bluetape4k.javers.examples.exposedddd.domain.CustomerId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderStatus
import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant

/**
 * 주문 CQRS 예제를 위한 Redis 기반 query-side read model입니다.
 *
 * ## 계약
 * instance는 domain event에서 파생되며 조회에 최적화됩니다. command-side의
 * source of truth는 아닙니다.
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
