package dev.pellet.server

import dev.pellet.logging.info
import dev.pellet.logging.pelletLogger
import dev.pellet.server.buffer.AlwaysAllocatingPelletBufferPool
import dev.pellet.server.codec.http.HTTPMessageCodec
import dev.pellet.server.codec.http.HTTPRequestMessage
import dev.pellet.server.connector.SocketConnector
import dev.pellet.server.metrics.PelletTimer
import dev.pellet.server.routing.http.HTTPRequestHandler
import dev.pellet.server.routing.http.HTTPRouting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PelletServer(
    private val logRequests: Boolean,
    private val connectors: List<PelletConnector>
) {

    private val logger = pelletLogger<PelletServer>()
    private val writePool = AlwaysAllocatingPelletBufferPool(4096)
    private val readPool = AlwaysAllocatingPelletBufferPool(4096)

    fun start(): CompletableFuture<Void> {
        if (connectors.isEmpty()) {
            throw RuntimeException("Please define at least one connector")
        }

        val startupTimer = PelletTimer()

        logger.info { "Pellet server starting..." }
        logger.info { "Get help, give feedback, and support development at https://www.pellet.dev" }

        val executors = Executors.newVirtualThreadPerTaskExecutor()
        val dispatcher = Dispatchers.Unconfined
        val supervisorContext = SupervisorJob()
        val scope = CoroutineScope(dispatcher + supervisorContext)

        val connectorJobs = connectors.map {
            when (it) {
                is PelletConnector.HTTP -> createHTTPConnectorJob(it, scope, executors)
            }
        }

        supervisorContext.invokeOnCompletion {
            logger.info(it) { "Pellet server stopped" }
        }

        val startupDuration = startupTimer.markAndReset()
        val startupMs = startupDuration.toMillis()
        logger.info { "Pellet started in ${startupMs}ms" }

        return CompletableFuture.allOf(*connectorJobs.toTypedArray())
    }

    private fun createHTTPConnectorJob(
        spec: PelletConnector.HTTP,
        scope: CoroutineScope,
        executorService: ExecutorService
    ): CompletableFuture<Void> {
        logger.info { "Starting connector: $spec" }
        validateAndPrintRoutes(spec.router)
        val connectorAddress = InetSocketAddress(spec.endpoint.hostname, spec.endpoint.port)
        val connector = SocketConnector(scope, executorService, connectorAddress, writePool) { client ->
            val output = HTTPRequestHandler(client, spec.router, writePool, logRequests)
            val channel = Channel<HTTPRequestMessage>()
            val codec = HTTPMessageCodec(channel, readPool)
            scope.launch {
                while (this.isActive) {
                    val message = channel.receive()
                    output.handle(message)
                }
            }
            codec
        }
        return connector.createAcceptJob()
    }

    private fun validateAndPrintRoutes(router: HTTPRouting) {
        if (router.routes.isEmpty()) {
            throw RuntimeException("routes must be defined before starting a connector")
        }
        logger.info { "Routes: \n${router.routes.joinToString("\n")}" }
    }
}
