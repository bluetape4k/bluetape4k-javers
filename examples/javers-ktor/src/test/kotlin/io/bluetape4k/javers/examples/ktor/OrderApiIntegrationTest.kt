package io.bluetape4k.javers.examples.ktor

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.javers.examples.ktor.domain.OrderPlaced
import io.bluetape4k.ktor.core.HealthResponse
import io.bluetape4k.ktor.testing.bluetape4kJsonClient
import io.bluetape4k.ktor.testing.decodeJsonBody
import io.bluetape4k.ktor.testing.shouldHaveStatus
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.atomic.AtomicInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderApiIntegrationTest {

    @Test
    fun `place order persists command state and first JaVers snapshot`() = testApplication {
        application {
            javersKtorModule(databaseName = newDatabaseName())
        }
        val client = bluetape4kJsonClient {
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
        }
        val orderId = newOrderId()

        val placed = client.post("/orders") {
            setBody(placeOrderRequest(orderId))
        }

        placed shouldHaveStatus HttpStatusCode.Created
        val body = placed.decodeJsonBody<OrderResponse>()
        body.orderId shouldBeEqualTo orderId
        body.status shouldBeEqualTo "PLACED"
        body.totalAmount shouldBeEqualTo "25.00"

        val history = client.get("/orders/$orderId/history")

        history shouldHaveStatus HttpStatusCode.OK
        val historyBody = history.decodeJsonBody<OrderHistoryResponse>()
        historyBody.snapshots shouldHaveSize 1
        historyBody.snapshots.single().domainEventType shouldBeEqualTo
            OrderPlaced::class.qualifiedName
    }

    @Test
    fun `mark order paid updates command state and creates second snapshot`() = testApplication {
        application {
            javersKtorModule(databaseName = newDatabaseName())
        }
        val client = bluetape4kJsonClient {
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
        }
        val orderId = newOrderId()
        client.post("/orders") {
            setBody(placeOrderRequest(orderId))
        } shouldHaveStatus HttpStatusCode.Created

        val paid = client.post("/orders/$orderId/paid") {
            setBody(MarkOrderPaidRequest(author = "tester"))
        }

        paid shouldHaveStatus HttpStatusCode.OK
        paid.decodeJsonBody<OrderResponse>().status shouldBeEqualTo "PAID"

        val lookup = client.get("/orders/$orderId")
        lookup shouldHaveStatus HttpStatusCode.OK
        lookup.decodeJsonBody<OrderResponse>().status shouldBeEqualTo "PAID"

        val history = client.get("/orders/$orderId/history?limit=200")
        history shouldHaveStatus HttpStatusCode.OK
        val historyBody = history.decodeJsonBody<OrderHistoryResponse>()
        historyBody.limit shouldBeEqualTo 100
        historyBody.snapshots shouldHaveSize 2
    }

    @Test
    fun `unknown order lookup returns not found`() = testApplication {
        application {
            javersKtorModule(databaseName = newDatabaseName())
        }
        val client = bluetape4kJsonClient()

        val response = client.get("/orders/${newOrderId()}")

        response shouldHaveStatus HttpStatusCode.NotFound
    }

    @Test
    fun `invalid place order payload returns bad request`() = testApplication {
        application {
            javersKtorModule(databaseName = newDatabaseName())
        }
        val client = bluetape4kJsonClient {
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
        }

        val response = client.post("/orders") {
            setBody(
                PlaceOrderRequest(
                    orderId = "",
                    author = "tester",
                    customerId = "customer-1",
                    items = listOf(
                        OrderItemRequest(sku = "sku-1", quantity = 0, unitPrice = "12.50"),
                    ),
                ),
            )
        }

        response shouldHaveStatus HttpStatusCode.BadRequest
    }

    @Test
    fun `health and readiness routes come from bluetape4k Ktor core`() = testApplication {
        application {
            javersKtorModule(databaseName = newDatabaseName())
        }
        val client = bluetape4kJsonClient()

        val health = client.get("/healthz")
        val readiness = client.get("/readyz")

        health shouldHaveStatus HttpStatusCode.OK
        readiness shouldHaveStatus HttpStatusCode.OK
        health.decodeJsonBody<HealthResponse>().status shouldBeEqualTo HealthResponse.UP
        readiness.decodeJsonBody<HealthResponse>().status shouldBeEqualTo HealthResponse.UP
    }

    private fun placeOrderRequest(orderId: String): PlaceOrderRequest {
        return PlaceOrderRequest(
            orderId = orderId,
            author = "tester",
            customerId = "customer-1",
            items = listOf(
                OrderItemRequest(sku = "sku-1", quantity = 2, unitPrice = "12.50"),
            ),
        )
    }

    private fun newOrderId(): String = "order-${Sequence.incrementAndGet()}"

    private fun newDatabaseName(): String = "javers-ktor-${Sequence.incrementAndGet()}"

    private companion object {
        private val Sequence = AtomicInteger()
    }
}
