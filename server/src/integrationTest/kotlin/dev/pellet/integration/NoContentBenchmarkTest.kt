package dev.pellet.integration

import dev.pellet.logging.info
import dev.pellet.logging.pelletLogger
import dev.pellet.server.PelletBuilder.pelletServer
import dev.pellet.server.PelletConnector
import dev.pellet.server.routing.http.HTTPRouteResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test

class NoContentBenchmarkTest {

    private val logger = pelletLogger<NoContentBenchmarkTest>()
    private val numberOfRequests = System.getProperty("benchmark.requests.total")?.toIntOrNull() ?: 1_000_000

    @Test
    fun `benchmark no content response`() = runBlocking {
        val counter = AtomicInteger(numberOfRequests)
        val pellet = pelletServer {
            logRequests = false
            httpConnector {
                endpoint = PelletConnector.Endpoint(
                    "127.0.0.1",
                    9001
                )
                router {
                    get("/") {
                        counter.decrementAndGet()
                        HTTPRouteResponse.Builder()
                            .noContent()
                            .build()
                    }
                }
            }
        }
        val job = pellet.start()

        val client = OkHttpClient().newBuilder()
            .connectionPool(ConnectionPool(30, 1L, TimeUnit.MINUTES))
            .build()
        client.dispatcher.maxRequestsPerHost = 30
        client.connectionPool.connectionCount()
        logger.info { "sending ${counter.get()} requests..." }

        val dispatcher = Dispatchers.Default
        val channel = Channel<Request>()
        val supervisor = SupervisorJob()
        val scope = CoroutineScope(dispatcher + supervisor)
        scope.launch {
            numberOfRequests.downTo(0).map {
                val request = Request.Builder()
                    .url("http://127.0.0.1:9001")
                    .get()
                    .build()
                channel.send(request)
            }
        }

        val processorCount = Runtime.getRuntime().availableProcessors()
        val startTime = Instant.now()
        processorCount.downTo(0).map {
            scope.async {
                sendRequests(client, channel)
            }
        }

        scope.async {
            while (this.isActive) {
                val count = counter.get()
                if (count <= 0) {
                    supervisor.cancel()
                    return@async
                }
                logger.info { "completed $count" }
                delay(1000L)
            }
        }.join()
        job.cancel()

        val endTime = Instant.now()
        val timeElapsedMs = Duration.between(startTime, endTime).toMillis()
        val rps = (numberOfRequests / timeElapsedMs.toDouble()) * 1000L
        logger.info { "completed rps: $rps" }
    }

    private suspend fun CoroutineScope.sendRequests(
        client: OkHttpClient,
        channel: Channel<Request>
    ) {
        while (this.isActive) {
            val request = channel.receive()
            val future = CompletableFuture<Response>()
            client.newCall(request).enqueue(toCallback(future))
            val response = future.await()
            assert(response.code == 204)
            yield()
        }
    }

    private fun toCallback(future: CompletableFuture<Response>): Callback {
        return object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                future.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: Response) {
                future.complete(response)
            }
        }
    }
}
