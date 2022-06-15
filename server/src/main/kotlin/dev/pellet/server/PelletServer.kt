package dev.pellet.server

import dev.pellet.logging.pelletLogger
import dev.pellet.server.buffer.AlwaysAllocatingPelletBufferPool
import dev.pellet.server.buffer.PelletBufferPooling
import dev.pellet.server.codec.http.HTTPMessageCodec
import dev.pellet.server.codec.http.WorkItem
import dev.pellet.server.metrics.PelletTimer
import dev.pellet.server.routing.http.HTTPRequestHandler
import dev.pellet.server.routing.http.HTTPRouting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.net.InetSocketAddress
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class PelletServer(
    private val logRequests: Boolean,
    private val connectors: List<PelletConnector>
) {

    private val logger = pelletLogger<PelletServer>()
    private val readPool = AlwaysAllocatingPelletBufferPool(4096)

    fun start() {
        if (connectors.isEmpty()) {
            throw RuntimeException("Please define at least one connector")
        }

        val startupTimer = PelletTimer()

        logger.info { "Pellet server starting..." }
        logger.info { "Get help, give feedback, and support development at https://www.pellet.dev" }

        val connectorJobs = connectors.map {
            when (it) {
                is PelletConnector.HTTP -> createHTTPNIOConnectorJob(it, readPool)
            }
        }

        // connectorJobs.forEach { job ->
        //     job.start()
        // }
        connectorJobs.forEach { connectorThreads ->
            connectorThreads.forEach {
                it.start()
            }
        }

        // supervisorContext.invokeOnCompletion {
        //     logger.info(it) { "Pellet server stopped" }
        // }

        val startupDuration = startupTimer.markAndReset()
        val startupMs = startupDuration.toMillis()
        logger.info { "Pellet started in ${startupMs}ms" }

        return connectorJobs.flatten().forEach {
            it.join()
        }
    }

    private fun createHTTPNIOConnectorJob(
        spec: PelletConnector.HTTP,
        pool: PelletBufferPooling
    ): List<Thread> {
        val connectorAddress = InetSocketAddress(spec.endpoint.hostname, spec.endpoint.port)
        val accepter = NIOSocketAccepter(readPool)
        val writeChannel = LinkedBlockingQueue<WriteItem>()
        val handler = HTTPRequestHandler(spec.router, pool, writeChannel, logRequests)
        val processorCount = Runtime.getRuntime().availableProcessors()
        val writerCount = 1
        // val numberOfProcessors = (processorCount - writerCount).coerceAtLeast(1)
        // todo: why does 1 processor perform better than 2, 3, etc?
        // todo: modify NIO bit to use NIO2
        val numberOfProcessors = 1
        val supervisorScope = SupervisorJob()
        val dispatcher = Dispatchers.Default
        val workers = processorCount
        val coroutineScope = CoroutineScope(dispatcher + supervisorScope)

        val workQueue = LinkedBlockingQueue<WorkItem>()
        val readSelectors = (0 until numberOfProcessors)
            .map {
                Selector.open()
            }

        val processorThreads = (0 until numberOfProcessors)
            .map { index ->
                thread(start = false, name = "processor $index") {
                    val processor = NIOSocketProcessor(pool, readSelectors[index], coroutineScope)
                    processor.run()
                }
            }
        val workerCoroutines = (0 until workers)
            .map {
                coroutineScope.launch {
                    while (isActive) {
                        val workItem = runInterruptible {
                            workQueue.take()
                        }
                        handler.handle(workItem.message, workItem.client)
                    }
                }
            }
        val writerThreads = (0 until writerCount)
            .map { index ->
                thread(start = false, name = "writer $index") {
                    val writer = NIOSocketWriter()
                    writer.run(writeChannel)
                }
            }
        val roundRobinIndex = AtomicInteger(0)
        val selector: (SocketChannel) -> Selector = {
            val nextIndex = roundRobinIndex.getAndIncrement()
            if (nextIndex > numberOfProcessors - 1) {
                roundRobinIndex.set(0)
                readSelectors[0]
            } else {
                readSelectors[nextIndex]
            }
        }

        val accepterThread = thread(
            start = false,
            name = "accepter"
        ) {
            accepter.run(
                connectorAddress,
                selector,
                codecFactory = {
                    HTTPMessageCodec(handler, pool, coroutineScope, workQueue)
                }
            )
        }

        return listOf(accepterThread) + processorThreads + writerThreads
    }
    //
    // private fun createHTTPConnectorJob(
    //     spec: PelletConnector.HTTP,
    //     scope: CoroutineScope,
    //     group: AsynchronousChannelGroup
    // ): Job {
    //     logger.info { "Starting connector: $spec" }
    //     validateAndPrintRoutes(spec.router)
    //     val connectorAddress = InetSocketAddress(spec.endpoint.hostname, spec.endpoint.port)
    //     val connector = SocketConnector(scope, group, connectorAddress, writePool) { client ->
    //         val output = HTTPRequestHandler(client, spec.router, writePool, logRequests)
    //         val codec = HTTPMessageCodec(output, readPool)
    //         codec
    //     }
    //     return connector.createAcceptJob()
    // }

    private fun validateAndPrintRoutes(router: HTTPRouting) {
        if (router.routes.isEmpty()) {
            throw RuntimeException("routes must be defined before starting a connector")
        }
        logger.info { "Routes: \n${router.routes.joinToString("\n")}" }
    }

    private fun calculateMaximumThreadCount(): Int {
        val numberOfCores = Runtime.getRuntime().availableProcessors()
        val waitTime = 50 // ms
        val processingTime = 5 // ms
        val threadCount = numberOfCores * (1 + (waitTime / processingTime.toDouble()))
        return threadCount
            .roundToInt()
            .coerceIn(numberOfCores, 1000)
    }
}
