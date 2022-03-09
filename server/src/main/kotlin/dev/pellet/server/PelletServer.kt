package dev.pellet.server

import dev.pellet.logging.info
import dev.pellet.logging.pelletLogger
import dev.pellet.server.buffer.AlwaysAllocatingPelletBufferPool
import dev.pellet.server.codec.http.HTTPMessageCodec
import dev.pellet.server.codec.http.HTTPRequestHandler
import dev.pellet.server.connector.SocketConnector
import dev.pellet.server.metrics.PelletTimer
import dev.pellet.server.routing.http.HTTPRouting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.net.InetSocketAddress
import kotlin.coroutines.CoroutineContext

class PelletServer(
    private val logRequests: Boolean,
    private val connectors: List<PelletConnector>
) {

    private val logger = pelletLogger<PelletServer>()
    private val writePool = AlwaysAllocatingPelletBufferPool(4096)
    private val readPool = AlwaysAllocatingPelletBufferPool(4096)

    fun start(): Job {
        if (connectors.isEmpty()) {
            throw RuntimeException("Please define at least one connector")
        }

        val startupTimer = PelletTimer()

        logger.info { "Pellet server starting..." }
        logger.info { "Get help, give feedback, and support development at https://www.pellet.dev" }

        val dispatcher = Dispatchers.Default
        val context = SupervisorJob()
        val scope = object : CoroutineScope {
            override val coroutineContext: CoroutineContext
                get() = dispatcher + context
        }

        val connectorJobs = connectors.map {
            when (it) {
                is PelletConnector.HTTP -> createHTTPConnectorJob(it, scope)
            }
        }

        connectorJobs.forEach { job ->
            job.start()
        }

        context.invokeOnCompletion {
            logger.info(it) { "Pellet server stopped" }
        }

        val startupDuration = startupTimer.markAndReset()
        val startupMs = startupDuration.toMillis()
        logger.info { "Pellet started in ${startupMs}ms" }

        return context
    }

    private fun createHTTPConnectorJob(
        spec: PelletConnector.HTTP,
        scope: CoroutineScope
    ): Job {
        logger.info { "Starting connector: $spec" }
        validateAndPrintRoutes(spec.router)
        val connectorAddress = InetSocketAddress(spec.endpoint.hostname, spec.endpoint.port)
        val connector = SocketConnector(scope, connectorAddress, writePool) { client ->
            val output = HTTPRequestHandler(client, spec.router, writePool, logRequests)
            val codec = HTTPMessageCodec(output, readPool)
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
