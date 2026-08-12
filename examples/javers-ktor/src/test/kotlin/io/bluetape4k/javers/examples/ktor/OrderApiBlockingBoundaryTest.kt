package io.bluetape4k.javers.examples.ktor

import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.ktor.testing.bluetape4kJsonClient
import io.bluetape4k.ktor.testing.shouldHaveStatus
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class OrderApiBlockingBoundaryTest {

    @Test
    fun `jdbc and JaVers route calls use the caller supplied blocking dispatcher`() {
        val dispatcher = RecordingBlockingDispatcher()
        try {
            testApplication {
                application {
                    javersKtorModule(
                        databaseName = "javers-ktor-boundary-${Sequence.incrementAndGet()}",
                        blockingDispatcher = dispatcher,
                    )
                }
                val client = bluetape4kJsonClient {
                    defaultRequest {
                        contentType(ContentType.Application.Json)
                    }
                }
                val orderId = "order-boundary-${Sequence.incrementAndGet()}"

                client.post("/orders") {
                    setBody(placeOrderRequest(orderId))
                } shouldHaveStatus HttpStatusCode.Created
                val afterCreate = dispatcher.dispatchCount.get()
                afterCreate shouldBeGreaterThan 0

                client.post("/orders/$orderId/paid") {
                    setBody(MarkOrderPaidRequest(author = "boundary-test"))
                } shouldHaveStatus HttpStatusCode.OK
                val afterPaid = dispatcher.dispatchCount.get()
                afterPaid shouldBeGreaterThan afterCreate

                client.get("/orders/$orderId") shouldHaveStatus HttpStatusCode.OK
                val afterLookup = dispatcher.dispatchCount.get()
                afterLookup shouldBeGreaterThan afterPaid

                client.get("/orders/$orderId/history") shouldHaveStatus HttpStatusCode.OK
                dispatcher.dispatchCount.get() shouldBeGreaterThan afterLookup
                dispatcher.threadNames.all { it.startsWith("javers-ktor-blocking-") }.shouldBeTrue()
            }
        } finally {
            dispatcher.close()
        }
    }

    @Test
    fun `blocking boundary propagates cancellation after the synchronous block exits`() = runSuspendIO {
        val dispatcher = RecordingBlockingDispatcher()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        try {
            coroutineScope {
                val job = launch {
                    withBlockingDispatcher(dispatcher) {
                        started.countDown()
                        release.await()
                    }
                }

                try {
                    started.await(1, TimeUnit.SECONDS).shouldBeTrue()
                    job.cancel()
                } finally {
                    release.countDown()
                    job.join()
                }

                job.isCancelled.shouldBeTrue()
            }
        } finally {
            release.countDown()
            dispatcher.close()
        }
    }

    private fun placeOrderRequest(orderId: String): PlaceOrderRequest {
        return PlaceOrderRequest(
            orderId = orderId,
            author = "boundary-test",
            customerId = "customer-boundary",
            items = listOf(
                OrderItemRequest(sku = "sku-boundary", quantity = 1, unitPrice = "9.99"),
            ),
        )
    }

    private class RecordingBlockingDispatcher : CoroutineDispatcher(), AutoCloseable {
        private val threadFactory = ThreadFactory { runnable ->
            Thread(runnable, "javers-ktor-blocking-${ThreadSequence.incrementAndGet()}").apply {
                isDaemon = true
            }
        }
        private val executor = Executors.newFixedThreadPool(2, threadFactory).asCoroutineDispatcher()

        val dispatchCount = AtomicInteger()
        val threadNames: MutableSet<String> = ConcurrentHashMap.newKeySet()

        override fun dispatch(context: kotlin.coroutines.CoroutineContext, block: Runnable) {
            dispatchCount.incrementAndGet()
            executor.dispatch(context) {
                threadNames += Thread.currentThread().name
                block.run()
            }
        }

        override fun close() {
            executor.close()
        }

        private companion object {
            val ThreadSequence = AtomicInteger()
        }
    }

    private companion object {
        val Sequence = AtomicInteger()
    }
}
