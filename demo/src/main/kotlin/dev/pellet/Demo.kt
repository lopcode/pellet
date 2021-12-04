package dev.pellet

import dev.pellet.logging.logger
import kotlinx.coroutines.runBlocking

object Demo

val logger = logger<Demo>()

fun main() = runBlocking {
    val connectors = listOf(
        PelletConnector.HTTP("localhost", 8082),
        PelletConnector.HTTP("localhost", 8083),
    )
    val pellet = PelletServer(connectors)
    val job = pellet.start { context, responder ->
        logger.debug("got request: ${context.message}")
        responder.writeNoContent()
    }
    job.join()
}
