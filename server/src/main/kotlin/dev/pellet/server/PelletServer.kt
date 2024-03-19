package dev.pellet.server

import dev.pellet.logging.pelletLogger
import dev.pellet.server.codec.http.HTTPMessageCodec
import dev.pellet.server.codec.http.HTTPRequestMessage
import dev.pellet.server.metrics.PelletTimer
import dev.pellet.server.routing.http.HTTPRequestHandler
import dev.pellet.server.routing.http.HTTPRouting
import dev.pellet.server.socket.BlockingSocketAccepter
import dev.pellet.server.socket.BlockingSocketHandler
import java.net.InetSocketAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit

class PelletServer(
    private val logRequests: Boolean,
    private val connectionConcurrencyLimit: Int = DEFAULT_MAX_CONNECTION_CONCURRENCY,
    private val connectors: List<PelletConnector>
) {

    companion object {

        const val DEFAULT_MAX_CONNECTION_CONCURRENCY = 16384
    }

    private val logger = pelletLogger<PelletServer>()

    fun start() {
        if (connectors.isEmpty()) {
            throw RuntimeException("Please define at least one connector")
        }

        val startupTimer = PelletTimer()

        logger.info { "Pellet server starting..." }
        logger.info { "Get help and give feedback at https://www.pellet.dev" }

        val virtualThreadFactory = Thread
            .ofVirtual()
            .name("pellet-virtual-", 0)
            .factory()
        val workerExecutorService = Executors.newThreadPerTaskExecutor(virtualThreadFactory)
        val supervisorExecutorService = Executors.newThreadPerTaskExecutor(Executors.defaultThreadFactory())

        val connectorJobs = connectors
            .map {
                when (it) {
                    is PelletConnector.HTTP -> createHTTPAccepterJob(
                        it,
                        connectionConcurrencyLimit,
                        supervisorExecutorService,
                        workerExecutorService
                    )
                }
            }
            .flatten()

        connectorJobs.forEach { job ->
            supervisorExecutorService.execute(job)
        }

        val startupDuration = startupTimer.markAndReset()
        val startupMs = startupDuration.toMillis()
        logger.info { "Pellet started in ${startupMs}ms" }

        while (!supervisorExecutorService.isShutdown) {
            try {
                supervisorExecutorService.awaitTermination(60L, TimeUnit.SECONDS)
            } catch (exception: InterruptedException) {
                supervisorExecutorService.shutdownNow()
            }
        }
        workerExecutorService.shutdownNow()
        workerExecutorService.awaitTermination(60L, TimeUnit.SECONDS)

        logger.info { "Pellet server stopped" }
        return
    }

    private fun createHTTPAccepterJob(
        spec: PelletConnector.HTTP,
        connectionConcurrencyLimit: Int,
        supervisorExecutorService: ExecutorService,
        workerExecutorService: ExecutorService
    ): List<Runnable> {
        logger.info { "Starting connector: $spec" }
        validateAndPrintRoutes(spec.router)

        val connectorAddress = InetSocketAddress(spec.endpoint.hostname, spec.endpoint.port)
        val socketHandlerFactory = { BlockingSocketHandler() }
        val accepter = BlockingSocketAccepter(
            workerExecutorService,
            socketHandlerFactory,
            connectionConcurrencyLimit
        )
        val handler = HTTPRequestHandler(spec.router, logRequests)
        val accepterJob = Runnable {
            acceptHTTPClients(
                accepter,
                connectorAddress,
                supervisorExecutorService,
                workerExecutorService,
                handler
            )
        }

        return listOf(accepterJob)
    }

    private fun acceptHTTPClients(
        accepter: BlockingSocketAccepter,
        connectorAddress: InetSocketAddress,
        supervisorExecutorService: ExecutorService,
        workerExecutorService: ExecutorService,
        handler: HTTPRequestHandler
    ) {
        val codecFactory = {
            HTTPMessageCodec { message, client ->
                try {
                    workerExecutorService.execute {
                        handleIncomingMessage(handler, client, message)
                    }
                } catch (exception: RejectedExecutionException) {
                    logger.warn(exception) { "failed to schedule handler, handling directly" }
                    handleIncomingMessage(handler, client, message)
                }
            }
        }
        val result = runCatching {
            accepter.accept(
                connectorAddress,
                codecFactory
            )
        }
        val exception = result.exceptionOrNull()
        if (exception != null && exception !is InterruptedException) {
            logger.error(exception) { "unexpected accepter failure" }
            supervisorExecutorService.shutdownNow()
        }
    }

    private fun handleIncomingMessage(
        handler: HTTPRequestHandler,
        client: PelletServerClient,
        message: HTTPRequestMessage
    ) {
        val handleResult = runCatching {
            handler.handle(message, client)
        }
        val exception = handleResult.exceptionOrNull()
        if (exception != null) {
            logger.warn(exception) { "failed to handle work for client $client " }
            client.close(
                CloseReason.ServerException(exception)
            )
        }
    }

    private fun validateAndPrintRoutes(router: HTTPRouting) {
        if (router.routes.isEmpty()) {
            throw RuntimeException("routes must be defined before starting a connector")
        }
        logger.info { "Routes: \n${router.routes.joinToString("\n")}" }
    }
}
