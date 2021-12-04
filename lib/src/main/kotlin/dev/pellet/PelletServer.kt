package dev.pellet

import dev.pellet.codec.http.HTTPMessageCodec
import dev.pellet.codec.http.HTTPRequestHandler
import dev.pellet.connector.SocketConnector
import dev.pellet.logging.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.net.InetSocketAddress
import kotlin.coroutines.CoroutineContext

val logger = logger<PelletServer>()

class PelletServer(
    private val connectors: List<PelletConnector>
) {

    fun start(
        action: suspend (PelletContext, PelletResponder) -> Unit
    ): Job {
        if (connectors.isEmpty()) {
            throw RuntimeException("Please define at least one connector")
        }

        logger.info("Pellet server starting...")
        logger.info("Please support development at https://www.pellet.dev/support")

        val dispatcher = Dispatchers.Default
        val context = SupervisorJob()
        val scope = object : CoroutineScope {
            override val coroutineContext: CoroutineContext
                get() = dispatcher + context
        }

        logger.info("Starting connectors:")
        connectors.forEach {
            logger.info("  $it")
        }

        val connectorJobs = connectors.map {
            when (it) {
                is PelletConnector.HTTP -> {
                    val connectorAddress = InetSocketAddress(it.hostname, it.port)
                    val connector = SocketConnector(scope, connectorAddress) { client ->
                        val output = HTTPRequestHandler(client, action)
                        val codec = HTTPMessageCodec(output)
                        codec
                    }
                    connector.createAcceptJob()
                }
            }
        }

        connectorJobs.forEach { job ->
            job.start()
        }

        context.invokeOnCompletion {
            logger.info("Pellet server stopped", it)
        }

        return context
    }
}
