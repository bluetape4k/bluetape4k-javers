package io.bluetape4k.javers.examples.exposedddd.service

import io.bluetape4k.javers.examples.exposedddd.domain.OrderId
import io.bluetape4k.javers.examples.exposedddd.projection.OrderSummary
import io.bluetape4k.javers.examples.exposedddd.projection.RedisOrderSummaryProjection

/**
 * Read-side API for querying order summaries from the Redis projection.
 *
 * ## Contract
 * This service does not hit the command-side Exposed tables or JaVers audit
 * snapshots. It reads only the Redis projection maintained by Kafka events.
 */
class OrderQueryService(
    private val projection: RedisOrderSummaryProjection,
) {

    fun findSummary(orderId: OrderId): OrderSummary? {
        return projection.findById(orderId)
    }
}
