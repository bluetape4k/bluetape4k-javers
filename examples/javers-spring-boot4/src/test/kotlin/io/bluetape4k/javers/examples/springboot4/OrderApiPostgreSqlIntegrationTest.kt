package io.bluetape4k.javers.examples.springboot4

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.codec.Base58
import io.bluetape4k.javers.examples.springboot4.web.MarkOrderPaidRequest
import io.bluetape4k.javers.examples.springboot4.web.OrderHistoryResponse
import io.bluetape4k.javers.examples.springboot4.web.OrderItemRequest
import io.bluetape4k.javers.examples.springboot4.web.OrderResponse
import io.bluetape4k.javers.examples.springboot4.web.PlaceOrderRequest
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
class OrderApiPostgreSqlIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `postgresql backed Spring routes push history limit into JaVers query`() {
        val orderId = "order-postgresql-${Base58.randomString(8)}"

        val placed = placeOrder(orderId)
        placed.response.status shouldBeEqualTo 201
        placed.readBody<OrderResponse>().status shouldBeEqualTo "PLACED"

        val paid = mockMvc.performJsonPost(
            "/orders/$orderId/paid",
            MarkOrderPaidRequest(author = "postgresql-test"),
        )
            .andReturn()
        paid.response.status shouldBeEqualTo 200
        paid.readBody<OrderResponse>().status shouldBeEqualTo "PAID"

        val history = mockMvc.perform(get("/orders/$orderId/history?limit=1")).andReturn()
        history.response.status shouldBeEqualTo 200
        val historyBody = history.readBody<OrderHistoryResponse>()
        historyBody.limit shouldBeEqualTo 1
        historyBody.snapshots shouldHaveSize 1
        historyBody.snapshots.single().state["status"] shouldBeEqualTo "PAID"
    }

    private fun placeOrder(orderId: String) =
        mockMvc.performJsonPost(
            path = "/orders",
            body = PlaceOrderRequest(
                orderId = orderId,
                author = "postgresql-test",
                customerId = "customer-postgresql",
                items = listOf(
                    OrderItemRequest(sku = "sku-postgresql", quantity = 2, unitPrice = BigDecimal("10.00")),
                ),
            ),
        ).andReturn()

    private fun MockMvc.performJsonPost(path: String, body: Any) =
        perform(
            post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )

    private inline fun <reified T: Any> org.springframework.test.web.servlet.MvcResult.readBody(): T =
        objectMapper.readValue(response.contentAsString, T::class.java)

    private companion object {
        private val postgres: PostgreSQLServer = PostgreSQLServer.Launcher.postgres

        @JvmStatic
        @DynamicPropertySource
        fun databaseProperties(registry: DynamicPropertyRegistry) {
            registry.add("javers.example.database.url") { postgres.jdbcUrl }
            registry.add("javers.example.database.driver") { PostgreSQLServer.DRIVER_CLASS_NAME }
            registry.add("javers.example.database.username") {
                postgres.username ?: PostgreSQLServer.USERNAME
            }
            registry.add("javers.example.database.password") {
                postgres.password ?: PostgreSQLServer.PASSWORD
            }
        }
    }
}
