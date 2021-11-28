package dev.skye.pellet.integration

import dev.skye.pellet.PelletConnector
import dev.skye.pellet.PelletServer
import dev.skye.pellet.logging.logger
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

    private val logger = logger<NoContentBenchmarkTest>()
    private val numberOfRequests = System.getProperty("benchmark.requests.total")?.toIntOrNull() ?: 1_000_000

    @Test
    fun `benchmark no content response`() = runBlocking {
        val connector = PelletConnector.HTTP(
            hostname = "127.0.0.1",
            port = 9001
        )
        val pellet = PelletServer(listOf(connector))
        val counter = AtomicInteger(numberOfRequests)
        val job = pellet.start { _, responder ->
            counter.decrementAndGet()
            responder.writeNoContent()
        }

        val client = OkHttpClient().newBuilder()
            .connectionPool(ConnectionPool(50, 1L, TimeUnit.MINUTES))
            .build()
        client.dispatcher.maxRequestsPerHost = 50
        client.connectionPool.connectionCount()
        logger.info("sending ${counter.get()} requests...")

        val processors = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        val dispatcher = Dispatchers.IO.limitedParallelism(processors)

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

        val startTime = Instant.now()
        processors.downTo(0).map {
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
                logger.info("completed $count")
                delay(1000L)
            }
        }.join()
        job.cancel()

        val endTime = Instant.now()
        val timeElapsedMs = Duration.between(startTime, endTime).toMillis()
        val rps = (numberOfRequests / timeElapsedMs.toDouble()) * 1000L
        logger.info("completed rps: $rps")
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
