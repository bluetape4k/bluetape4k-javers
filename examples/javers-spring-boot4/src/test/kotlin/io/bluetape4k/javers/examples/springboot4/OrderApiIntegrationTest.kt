package io.bluetape4k.javers.examples.springboot4

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.javers.examples.springboot4.domain.OrderPlaced
import io.bluetape4k.javers.examples.springboot4.web.MarkOrderPaidRequest
import io.bluetape4k.javers.examples.springboot4.web.OrderHistoryResponse
import io.bluetape4k.javers.examples.springboot4.web.OrderItemRequest
import io.bluetape4k.javers.examples.springboot4.web.OrderResponse
import io.bluetape4k.javers.examples.springboot4.web.PlaceOrderRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class OrderApiIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `place order persists command state and first JaVers snapshot`() {
        val orderId = newOrderId()

        val placed = placeOrder(orderId)

        placed.response.status shouldBeEqualTo 201
        val body = placed.readBody<OrderResponse>()
        body.orderId shouldBeEqualTo orderId
        body.status shouldBeEqualTo "PLACED"
        body.totalAmount shouldBeEqualTo BigDecimal("25.00")

        val history = mockMvc.perform(get("/orders/$orderId/history")).andReturn()

        history.response.status shouldBeEqualTo 200
        val historyBody = history.readBody<OrderHistoryResponse>()
        historyBody.snapshots.size shouldBeEqualTo 1
        historyBody.snapshots.single().domainEventType shouldBeEqualTo OrderPlaced::class.qualifiedName
    }

    @Test
    fun `mark order paid updates command state and creates second snapshot`() {
        val orderId = newOrderId()
        placeOrder(orderId)

        val paid = mockMvc.performJsonPost("/orders/$orderId/paid", MarkOrderPaidRequest(author = "tester"))
            .andReturn()

        paid.response.status shouldBeEqualTo 200
        paid.readBody<OrderResponse>().status shouldBeEqualTo "PAID"

        val lookup = mockMvc.perform(get("/orders/$orderId")).andReturn()
        lookup.response.status shouldBeEqualTo 200
        lookup.readBody<OrderResponse>().status shouldBeEqualTo "PAID"

        val history = mockMvc.perform(get("/orders/$orderId/history?limit=200")).andReturn()
        history.response.status shouldBeEqualTo 200
        val historyBody = history.readBody<OrderHistoryResponse>()
        historyBody.limit shouldBeEqualTo 100
        historyBody.snapshots.size shouldBeEqualTo 2
    }

    @Test
    fun `unknown order lookup returns not found`() {
        val response = mockMvc.perform(get("/orders/${newOrderId()}")).andReturn()

        response.response.status shouldBeEqualTo 404
    }

    @Test
    fun `invalid place order payload returns client error`() {
        val response = mockMvc.performJsonPost(
            path = "/orders",
            body =
            PlaceOrderRequest(
                orderId = "",
                author = "tester",
                customerId = "customer-1",
                items = listOf(OrderItemRequest(sku = "sku-1", quantity = 0, unitPrice = BigDecimal("12.50"))),
            ),
        ).andReturn()

        response.response.status shouldBeEqualTo 400
    }

    private fun placeOrder(orderId: String) =
        mockMvc.performJsonPost(
            path = "/orders",
            body = PlaceOrderRequest(
                orderId = orderId,
                author = "tester",
                customerId = "customer-1",
                items = listOf(OrderItemRequest(sku = "sku-1", quantity = 2, unitPrice = BigDecimal("12.50"))),
            ),
        ).andReturn()

    private fun MockMvc.performJsonPost(path: String, body: Any) =
        perform(
            post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )

    private inline fun <reified T: Any> org.springframework.test.web.servlet.MvcResult.readBody(): T =
        objectMapper.readValue(response.contentAsString.shouldNotBeNull(), T::class.java)

    private fun newOrderId(): String = "order-${UUID.randomUUID()}"
}
