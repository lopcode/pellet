package dev.skye.pellet

import dev.skye.pellet.logging.logger
import kotlinx.coroutines.runBlocking

object Demo

val logger = logger<Demo>()

fun main() = runBlocking {
    logger.info("Pellet demo starting...")
    val pellet = Pellet("localhost", 8082)
    val job = pellet.start { context, responder ->
        logger.debug("got request: ${context.message}")
        responder.writeNoContent()
    }
    job.join()
    logger.info("Pellet demo ended")
}
