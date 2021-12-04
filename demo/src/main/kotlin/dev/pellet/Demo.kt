package dev.pellet

import dev.pellet.logging.logger
import dev.pellet.responder.http.HTTPRoute
import dev.pellet.responder.http.PelletHTTPContext
import dev.pellet.responder.http.PelletHTTPResponder
import dev.pellet.responder.http.PelletHTTPRouter
import kotlinx.coroutines.runBlocking

object Demo

val logger = logger<Demo>()

fun main() = runBlocking {
    val endpoint = PelletConnector.Endpoint("localhost", 8082)
    val route = HTTPRoute("/", ::handleRequest)
    val router = PelletHTTPRouter(
        listOf(route)
    )
    val connectors = listOf(
        PelletConnector.HTTP(endpoint, router),
        PelletConnector.HTTP(endpoint.copy(port = 8083), router),
    )
    val pellet = PelletServer(connectors)
    val job = pellet.start()
    job.join()
}

private suspend fun handleRequest(
    context: PelletHTTPContext,
    responder: PelletHTTPResponder
) {
    logger.debug("got request: ${context.message}")
    responder.writeNoContent()
}
