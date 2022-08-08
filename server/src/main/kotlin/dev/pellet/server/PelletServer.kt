package dev.pellet.server

import dev.pellet.logging.pelletLogger
import dev.pellet.server.buffer.AlwaysAllocatingPelletBufferPool
import dev.pellet.server.buffer.PelletBufferPooling
import dev.pellet.server.codec.http.HTTPMessageCodec
import dev.pellet.server.codec.http.HTTPRequestMessage
import dev.pellet.server.metrics.PelletTimer
import dev.pellet.server.nio.NIOSocketAccepter
import dev.pellet.server.nio.NIOSocketProcessor
import dev.pellet.server.routing.http.HTTPRequestHandler
import dev.pellet.server.routing.http.HTTPRouting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import java.net.InetSocketAddress
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.atomic.AtomicInteger

class PelletServer(
    private val logRequests: Boolean,
    private val maxWorkerCount: Int,
    private val connectors: List<PelletConnector>
) {

    private val logger = pelletLogger<PelletServer>()
    private val pool = AlwaysAllocatingPelletBufferPool(4096)

    fun start(): Job {
        if (connectors.isEmpty()) {
            throw RuntimeException("Please define at least one connector")
        }
        val maxWorkerCount = maxWorkerCount.coerceIn(
            minimumValue = connectors.size * 4,
            maximumValue = Runtime.getRuntime().availableProcessors() * 10
        )

        val startupTimer = PelletTimer()

        logger.info { "Pellet server starting..." }
        logger.info { "Get help, give feedback, and support development at https://www.pellet.dev" }

        val supervisorContext = SupervisorJob()
        val workerDispatcher = newFixedThreadPoolContext(maxWorkerCount, "worker")
        val connectorJobs = connectors
            .map {
                when (it) {
                    is PelletConnector.HTTP -> createHTTPNIOConnectorJob(it, pool, supervisorContext, workerDispatcher)
                }
            }
            .flatten()

        connectorJobs.forEach { job ->
            job.start()
        }

        supervisorContext.invokeOnCompletion {
            workerDispatcher.close()
            logger.info(it) { "Pellet server stopped" }
        }

        val startupDuration = startupTimer.markAndReset()
        val startupMs = startupDuration.toMillis()
        logger.info { "Pellet started in ${startupMs}ms" }

        return supervisorContext
    }

    private fun createHTTPNIOConnectorJob(
        spec: PelletConnector.HTTP,
        pool: PelletBufferPooling,
        supervisorContext: CompletableJob,
        parentDispatcher: ExecutorCoroutineDispatcher
    ): List<Job> {
        logger.info { "Starting connector: $spec" }
        validateAndPrintRoutes(spec.router)

        val connectorAddress = InetSocketAddress(spec.endpoint.hostname, spec.endpoint.port)
        val accepter = NIOSocketAccepter(pool)
        val handler = HTTPRequestHandler(spec.router, pool, logRequests)
        val nioProcessorCount = 1
        val coroutineScope = CoroutineScope(parentDispatcher + supervisorContext)
        val workerScope = CoroutineScope(parentDispatcher + supervisorContext)
        val readSelectors = (0 until nioProcessorCount)
            .map {
                Selector.open()
            }
        val processorJobs = (0 until nioProcessorCount)
            .map { index ->
                val selector = readSelectors[index]
                val processor = NIOSocketProcessor(pool, selector)
                processor.run(coroutineScope)
            }
        val roundRobinIndex = AtomicInteger(0)
        val selector: (SocketChannel) -> Selector = {
            val nextIndex = roundRobinIndex.getAndIncrement()
            if (nextIndex + 1 >= readSelectors.size) {
                roundRobinIndex.set(0)
            }
            readSelectors[nextIndex]
        }

        val accepterJob = coroutineScope.launch(
            start = CoroutineStart.LAZY,
            context = CoroutineName("accepter")
        ) {
            acceptHTTPClients(accepter, connectorAddress, selector, pool, supervisorContext, workerScope, handler)
        }

        return processorJobs + listOf(accepterJob)
    }

    private suspend fun acceptHTTPClients(
        accepter: NIOSocketAccepter,
        connectorAddress: InetSocketAddress,
        selector: (SocketChannel) -> Selector,
        pool: PelletBufferPooling,
        supervisorContext: CompletableJob,
        scope: CoroutineScope,
        handler: HTTPRequestHandler
    ) {
        val result = runCatching {
            accepter.run(
                connectorAddress,
                selector,
                codecFactory = {
                    HTTPMessageCodec(pool) { message, client ->
                        scope.launch {
                            handleIncomingMessage(handler, client, message)
                        }
                    }
                }
            )
        }
        val exception = result.exceptionOrNull()
        if (exception != null && exception !is CancellationException) {
            logger.error(exception) { "unexpected accepter failure" }
            supervisorContext.cancel()
        }
    }

    private suspend fun handleIncomingMessage(
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
