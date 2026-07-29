package io.bluetape4k.javers.examples.exposedddd.service

import io.bluetape4k.javers.examples.exposedddd.domain.OrderId
import io.bluetape4k.javers.examples.exposedddd.projection.OrderSummary
import io.bluetape4k.javers.examples.exposedddd.projection.RedisOrderSummaryProjection

/**
 * Redis projection에서 주문 summary를 조회하는 read-side API입니다.
 *
 * ## 계약
 * 이 service는 command-side Exposed table이나 JaVers audit snapshot을 조회하지
 * 않습니다. Kafka event로 유지되는 Redis projection만 읽습니다.
 */
class OrderQueryService(
    private val projection: RedisOrderSummaryProjection,
) {

    fun findSummary(orderId: OrderId): OrderSummary? {
        return projection.findById(orderId)
    }
}
