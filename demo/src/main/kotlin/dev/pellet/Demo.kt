package dev.pellet

import dev.pellet.PelletBuilder.httpRouter
import dev.pellet.PelletBuilder.pelletServer
import dev.pellet.logging.logger
import dev.pellet.responder.http.PelletHTTPContext
import dev.pellet.responder.http.PelletHTTPResponder
import kotlinx.coroutines.runBlocking

object Demo

val logger = logger<Demo>()

fun main() = runBlocking {
    val sharedRouter = httpRouter {
        get("/", ::handleRequest)
        post("/v1/hello", ::handleRequest)
    }
    val pellet = pelletServer {
        httpConnector {
            endpoint = PelletConnector.Endpoint(
                hostname = "localhost",
                port = 8082
            )
            router = sharedRouter
        }
        httpConnector {
            endpoint = PelletConnector.Endpoint(
                hostname = "localhost",
                port = 8083
            )
            router = sharedRouter
        }
    }
    pellet.start().join()
}

fun simpleMain() = runBlocking {
    val pellet = pelletServer {
        httpConnector {
            endpoint = PelletConnector.Endpoint(
                hostname = "localhost",
                port = 8082
            )
            router {
                get("/v1/hello") { _, responder ->
                    responder.writeNoContent()
                }
            }
        }
    }
    pellet.start().join()
}

private suspend fun handleRequest(
    context: PelletHTTPContext,
    responder: PelletHTTPResponder
) {
    logger.debug("got request: ${context.message}")
    responder.writeNoContent()
}
