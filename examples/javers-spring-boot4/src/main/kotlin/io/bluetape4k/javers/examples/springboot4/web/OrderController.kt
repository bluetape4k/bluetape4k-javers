package io.bluetape4k.javers.examples.springboot4.web

import io.bluetape4k.javers.ddd.DOMAIN_EVENT_TYPE_PROPERTY
import io.bluetape4k.javers.examples.springboot4.domain.CustomerId
import io.bluetape4k.javers.examples.springboot4.domain.MarkOrderPaidCommand
import io.bluetape4k.javers.examples.springboot4.domain.Order
import io.bluetape4k.javers.examples.springboot4.domain.OrderId
import io.bluetape4k.javers.examples.springboot4.domain.OrderItem
import io.bluetape4k.javers.examples.springboot4.domain.PlaceOrderCommand
import io.bluetape4k.javers.examples.springboot4.persistence.OrderRepository
import io.bluetape4k.javers.examples.springboot4.service.OrderCommandHandler
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant

/**
 * REST endpoints for the Spring Boot 4 JaVers order example.
 */
@Validated
@RestController
@RequestMapping("/orders")
class OrderController(
    private val handler: OrderCommandHandler,
    private val repository: OrderRepository,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun placeOrder(@Valid @RequestBody request: PlaceOrderRequest): OrderResponse {
        val saved = handler.handle(request.toCommand())
        return saved.toResponse()
    }

    @PostMapping("/{orderId}/paid")
    fun markPaid(
        @PathVariable orderId: String,
        @Valid @RequestBody request: MarkOrderPaidRequest,
    ): OrderResponse {
        repository.load(OrderId(orderId))
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: $orderId")

        val paid = try {
            handler.handle(MarkOrderPaidCommand(orderId = OrderId(orderId), author = request.author))
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
        }
        return paid.toResponse()
    }

    @GetMapping("/{orderId}")
    fun findOrder(@PathVariable orderId: String): OrderResponse {
        val order = repository.load(OrderId(orderId))
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: $orderId")
        return order.toResponse()
    }

    @GetMapping("/{orderId}/history")
    fun findHistory(
        @PathVariable orderId: String,
        @RequestParam(defaultValue = "20") limit: Int,
    ): OrderHistoryResponse {
        val boundedLimit = limit.coerceIn(1, MAX_HISTORY_LIMIT)
        val history = repository.loadHistory(OrderId(orderId))
        if (history.isEmpty()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Order history not found: $orderId")
        }
        val snapshots = history.take(boundedLimit)
            .map { snapshot ->
                OrderSnapshotResponse(
                    version = snapshot.version,
                    commitId = snapshot.commitId.value(),
                    author = snapshot.commitMetadata.author,
                    committedAt = snapshot.commitMetadata.commitDateInstant,
                    domainEventType = snapshot.commitMetadata.properties[DOMAIN_EVENT_TYPE_PROPERTY],
                    changed = snapshot.changed,
                    state = snapshot.state.propertyNames.associateWith { property ->
                        snapshot.getPropertyValue(property)?.toString()
                    },
                )
            }
        return OrderHistoryResponse(orderId = orderId, limit = boundedLimit, snapshots = snapshots)
    }

    private fun PlaceOrderRequest.toCommand(): PlaceOrderCommand {
        return PlaceOrderCommand(
            orderId = OrderId(orderId),
            author = author,
            customerId = CustomerId(customerId),
            items = items.map { item ->
                OrderItem(
                    sku = item.sku,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                )
            },
        )
    }

    private fun Order.toResponse(): OrderResponse {
        return OrderResponse(
            orderId = id.value,
            customerId = customerId.value,
            status = status.name,
            totalAmount = totalAmount,
            items = items.map { item ->
                OrderItemResponse(
                    sku = item.sku,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    lineTotal = item.lineTotal,
                )
            },
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private companion object {
        private const val MAX_HISTORY_LIMIT = 100
    }
}

/**
 * Request body for placing a new order.
 */
data class PlaceOrderRequest(
    @field:NotBlank
    val orderId: String,
    @field:NotBlank
    val author: String,
    @field:NotBlank
    val customerId: String,
    @field:NotEmpty
    @field:Valid
    val items: List<OrderItemRequest>,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Request body item for placing an order.
 */
data class OrderItemRequest(
    @field:NotBlank
    val sku: String,
    @field:Positive
    val quantity: Int,
    @field:DecimalMin(value = "0.00", inclusive = false)
    val unitPrice: BigDecimal,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Request body for marking an order as paid.
 */
data class MarkOrderPaidRequest(
    @field:NotBlank
    val author: String,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * REST representation of the current command-side order state.
 */
data class OrderResponse(
    val orderId: String,
    val customerId: String,
    val status: String,
    val totalAmount: BigDecimal,
    val items: List<OrderItemResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * REST representation of one order line.
 */
data class OrderItemResponse(
    val sku: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * REST response containing bounded JaVers snapshot history for an order.
 */
data class OrderHistoryResponse(
    val orderId: String,
    val limit: Int,
    val snapshots: List<OrderSnapshotResponse>,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * REST representation of one JaVers snapshot.
 */
data class OrderSnapshotResponse(
    val version: Long,
    val commitId: String,
    val author: String,
    val committedAt: Instant,
    val domainEventType: String?,
    val changed: List<String>,
    val state: Map<String, String?>,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
