package io.bluetape4k.javers.examples.ktor

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.ktor.testing.bluetape4kJsonClient
import io.bluetape4k.ktor.testing.decodeJsonBody
import io.bluetape4k.ktor.testing.shouldHaveStatus
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.atomic.AtomicInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderApiPostgreSqlIntegrationTest {

    private val postgres: PostgreSQLServer = PostgreSQLServer.Launcher.postgres

    @Test
    fun `postgresql backed Ktor routes persist command state and JaVers history`() = testApplication {
        val database = Database.connect(
            url = postgres.jdbcUrl,
            driver = PostgreSQLServer.DRIVER_CLASS_NAME,
            user = postgres.username ?: PostgreSQLServer.USERNAME,
            password = postgres.password ?: PostgreSQLServer.PASSWORD,
        )
        application {
            javersKtorModule(
                database = database,
                databaseName = "javers-ktor-postgresql",
            )
        }
        val client = bluetape4kJsonClient {
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
        }
        val orderId = "order-postgresql-${Sequence.incrementAndGet()}"

        val placed = client.post("/orders") {
            setBody(
                PlaceOrderRequest(
                    orderId = orderId,
                    author = "postgresql-test",
                    customerId = "customer-postgresql",
                    items = listOf(
                        OrderItemRequest(sku = "sku-postgresql", quantity = 2, unitPrice = "10.00"),
                    ),
                ),
            )
        }.shouldHaveStatus(HttpStatusCode.Created)
            .decodeJsonBody<OrderResponse>()

        placed.orderId shouldBeEqualTo orderId
        placed.status shouldBeEqualTo "PLACED"

        val history = client.get("/orders/$orderId/history")
            .shouldHaveStatus(HttpStatusCode.OK)
            .decodeJsonBody<OrderHistoryResponse>()

        history.orderId shouldBeEqualTo orderId
        history.snapshots shouldHaveSize 1
    }

    private companion object {
        val Sequence = AtomicInteger()
    }
}
