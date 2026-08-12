package io.bluetape4k.javers.examples.ktor

import io.bluetape4k.javers.ddd.DOMAIN_EVENT_TYPE_PROPERTY
import io.bluetape4k.javers.ddd.DomainEventPublisher
import io.bluetape4k.javers.ddd.NoopDomainEventPublisher
import io.bluetape4k.javers.examples.ktor.domain.CustomerId
import io.bluetape4k.javers.examples.ktor.domain.MarkOrderPaidCommand
import io.bluetape4k.javers.examples.ktor.domain.Order
import io.bluetape4k.javers.examples.ktor.domain.OrderId
import io.bluetape4k.javers.examples.ktor.domain.OrderItem
import io.bluetape4k.javers.examples.ktor.domain.PlaceOrderCommand
import io.bluetape4k.javers.examples.ktor.persistence.OrderRepository
import io.bluetape4k.javers.examples.ktor.persistence.OrdersTable
import io.bluetape4k.javers.examples.ktor.service.OrderCommandHandler
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepository
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTable
import io.bluetape4k.javers.persistence.exposed.schema.CommitTable
import io.bluetape4k.ktor.core.installBluetape4kKtorCore
import io.bluetape4k.ktor.core.requiredPathParameter
import io.bluetape4k.ktor.core.respondApiError
import io.bluetape4k.support.requireNotBlank
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.Serializable as JavaSerializable
import java.math.BigDecimal
import java.time.Clock

private const val DEFAULT_HISTORY_LIMIT = 20
private const val MAX_HISTORY_LIMIT = 100
private val DatabaseNamePattern = Regex("[A-Za-z0-9_-]+")

/**
 * Ktor JaVers audit 예제 application을 설정합니다.
 *
 * ## 계약
 * 이 module은 Exposed JDBC, JaVers, Ktor route를 명시적으로 연결합니다. 예제
 * 내부 설정이며 production auto-configuration을 제공하지 않습니다.
 * [database]를 생략하면 module이 H2 데이터베이스를 생성하며, 전달한 데이터베이스는
 * 호출자가 소유합니다. 동기 JDBC와 JaVers 호출은 [blockingDispatcher]에서 실행합니다.
 *
 * @param databaseName H2 기본 데이터베이스 이름입니다.
 * @param eventPublisher 저장과 JaVers commit 뒤에 호출할 동기 domain-event publisher입니다.
 * @param clock command handler가 사용할 시계입니다.
 * @param database 호출자가 소유한 JDBC 데이터베이스입니다. 생략하면 H2를 생성합니다.
 * @param blockingDispatcher JDBC와 JaVers blocking 호출에 사용할 dispatcher입니다.
 */
fun Application.javersKtorModule(
    databaseName: String = "javers-ktor",
    eventPublisher: DomainEventPublisher = NoopDomainEventPublisher,
    clock: Clock = Clock.systemUTC(),
    database: Database? = null,
    blockingDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val services = createExampleServices(databaseName, database, eventPublisher, clock)

    installBluetape4kKtorCore()
    routing {
        orderRoutes(services.handler, services.repository, blockingDispatcher)
    }
}

fun main() {
    embeddedServer(CIO, host = "0.0.0.0", port = 8080) {
        javersKtorModule()
    }.start(wait = true)
}

private fun createExampleServices(
    databaseName: String,
    database: Database?,
    eventPublisher: DomainEventPublisher,
    clock: Clock,
): ExampleServices {
    databaseName.requireSafeDatabaseName()
    val resolvedDatabase = database ?: Database.connect(
        url = "jdbc:h2:mem:$databaseName;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
    )
    transaction(resolvedDatabase) {
        SchemaUtils.create(CommitTable, CdoSnapshotTable, OrdersTable)
    }
    val javers = JaversBuilder.javers()
        .registerJaversRepository(ExposedCdoSnapshotRepository(resolvedDatabase))
        .registerEntity(Order::class.java)
        .build()
    val repository = OrderRepository(resolvedDatabase, javers, eventPublisher)
    return ExampleServices(
        repository = repository,
        handler = OrderCommandHandler(repository, clock),
    )
}

private class ExampleServices(
    val repository: OrderRepository,
    val handler: OrderCommandHandler,
)

internal fun Routing.orderRoutes(
    handler: OrderCommandHandler,
    repository: OrderRepository,
    blockingDispatcher: CoroutineDispatcher,
) {
    route("/orders") {
        post {
            val request = call.receive<PlaceOrderRequest>()
            val saved = withBlockingDispatcher(blockingDispatcher) {
                handler.handle(request.toCommand())
            }
            call.respond(HttpStatusCode.Created, saved.toResponse())
        }

        post("/{orderId}/paid") {
            val orderId = call.requiredPathParameter("orderId")
            val request = call.receive<MarkOrderPaidRequest>()
            val id = OrderId(orderId)
            val existing = withBlockingDispatcher(blockingDispatcher) {
                repository.load(id)
            }
            if (existing == null) {
                call.respondOrderNotFound(orderId)
                return@post
            }
            val command = MarkOrderPaidCommand(
                orderId = id,
                author = request.author.requireNotBlank("author"),
            )
            val paid = withBlockingDispatcher(blockingDispatcher) {
                handler.handle(command)
            }
            call.respond(paid.toResponse())
        }

        get("/{orderId}") {
            val orderId = call.requiredPathParameter("orderId")
            val order = withBlockingDispatcher(blockingDispatcher) {
                repository.load(OrderId(orderId))
            }
            if (order == null) {
                call.respondOrderNotFound(orderId)
                return@get
            }
            call.respond(order.toResponse())
        }

        get("/{orderId}/history") {
            val orderId = call.requiredPathParameter("orderId")
            val boundedLimit = call.historyLimit()
            val history = withBlockingDispatcher(blockingDispatcher) {
                repository.loadHistory(OrderId(orderId), boundedLimit)
            }
            if (history.isEmpty()) {
                call.respondOrderHistoryNotFound(orderId)
                return@get
            }
            val snapshots = history.map { snapshot ->
                OrderSnapshotResponse(
                    version = snapshot.version,
                    commitId = snapshot.commitId.value(),
                    author = snapshot.commitMetadata.author,
                    committedAt = snapshot.commitMetadata.commitDateInstant.toString(),
                    domainEventType = snapshot.commitMetadata.properties[DOMAIN_EVENT_TYPE_PROPERTY],
                    changed = snapshot.changed,
                    state = snapshot.state.propertyNames.associateWith { property ->
                        snapshot.getPropertyValue(property)?.toString()
                    },
                )
            }
            call.respond(
                OrderHistoryResponse(
                    orderId = orderId,
                    limit = boundedLimit,
                    snapshots = snapshots,
                ),
            )
        }
    }
}

/** 동기 JDBC와 JaVers 호출을 호출자가 제공한 blocking dispatcher에서 실행합니다. */
internal suspend fun <T> withBlockingDispatcher(
    blockingDispatcher: CoroutineDispatcher,
    block: () -> T,
): T = try {
    withContext(blockingDispatcher) {
        block()
    }
} catch (e: CancellationException) {
    throw e
}

private suspend fun ApplicationCall.respondOrderNotFound(orderId: String) {
    respondApiError(
        status = HttpStatusCode.NotFound,
        error = "not_found",
        message = "Order not found: $orderId",
    )
}

private suspend fun ApplicationCall.respondOrderHistoryNotFound(orderId: String) {
    respondApiError(
        status = HttpStatusCode.NotFound,
        error = "not_found",
        message = "Order history not found: $orderId",
    )
}

private fun ApplicationCall.historyLimit(): Int {
    val rawValue = request.queryParameters["limit"] ?: return DEFAULT_HISTORY_LIMIT
    val parsed = rawValue.toIntOrNull()
        ?: throw IllegalArgumentException("Query parameter 'limit' must be an integer.")
    return parsed.coerceIn(1, MAX_HISTORY_LIMIT)
}

private fun PlaceOrderRequest.toCommand(): PlaceOrderCommand {
    return PlaceOrderCommand(
        orderId = OrderId(orderId.requireNotBlank("orderId")),
        author = author.requireNotBlank("author"),
        customerId = CustomerId(customerId.requireNotBlank("customerId")),
        items = items.requireNotEmpty("items").map { item ->
            OrderItem(
                sku = item.sku.requireNotBlank("sku"),
                quantity = item.quantity.requirePositiveQuantity(),
                unitPrice = item.unitPrice.requirePositiveBigDecimal("unitPrice"),
            )
        },
    )
}

private fun Order.toResponse(): OrderResponse {
    return OrderResponse(
        orderId = id.value,
        customerId = customerId.value,
        status = status.name,
        totalAmount = totalAmount.toPlainString(),
        items = items.map { item ->
            OrderItemResponse(
                sku = item.sku,
                quantity = item.quantity,
                unitPrice = item.unitPrice.toPlainString(),
                lineTotal = item.lineTotal.toPlainString(),
            )
        },
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )
}

private fun <T> List<T>.requireNotEmpty(parameterName: String): List<T> {
    require(isNotEmpty()) { "$parameterName must not be empty" }
    return this
}

private fun String.requireSafeDatabaseName(): String {
    requireNotBlank("databaseName")
    require(DatabaseNamePattern.matches(this)) {
        "databaseName must contain only letters, numbers, underscore, or hyphen"
    }
    return this
}

private fun Int.requirePositiveQuantity(): Int {
    require(this > 0) { "quantity must be positive" }
    return this
}

private fun String.requirePositiveBigDecimal(parameterName: String): BigDecimal {
    requireNotBlank(parameterName)
    val value = try {
        BigDecimal(this)
    } catch (e: NumberFormatException) {
        throw IllegalArgumentException("$parameterName must be a decimal number", e)
    }
    require(value > BigDecimal.ZERO) { "$parameterName must be positive" }
    return value
}

/**
 * 새 주문 생성 요청 body입니다.
 */
@Serializable
data class PlaceOrderRequest(
    val orderId: String,
    val author: String,
    val customerId: String,
    val items: List<OrderItemRequest>,
): JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * 주문 생성 요청에 포함되는 item body입니다.
 */
@Serializable
data class OrderItemRequest(
    val sku: String,
    val quantity: Int,
    val unitPrice: String,
): JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * 주문을 결제 완료로 표시하는 요청 body입니다.
 */
@Serializable
data class MarkOrderPaidRequest(
    val author: String,
): JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * 현재 command-side 주문 상태의 REST 표현입니다.
 */
@Serializable
data class OrderResponse(
    val orderId: String,
    val customerId: String,
    val status: String,
    val totalAmount: String,
    val items: List<OrderItemResponse>,
    val createdAt: String,
    val updatedAt: String,
): JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * 주문 line 하나의 REST 표현입니다.
 */
@Serializable
data class OrderItemResponse(
    val sku: String,
    val quantity: Int,
    val unitPrice: String,
    val lineTotal: String,
): JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * 주문의 제한된 JaVers snapshot 이력을 담는 REST 응답입니다.
 */
@Serializable
data class OrderHistoryResponse(
    val orderId: String,
    val limit: Int,
    val snapshots: List<OrderSnapshotResponse>,
): JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * JaVers snapshot 하나의 REST 표현입니다.
 */
@Serializable
data class OrderSnapshotResponse(
    val version: Long,
    val commitId: String,
    val author: String,
    val committedAt: String,
    val domainEventType: String?,
    val changed: List<String>,
    val state: Map<String, String?>,
): JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
