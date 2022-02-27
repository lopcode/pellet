package dev.pellet.demo

import dev.pellet.logging.pelletLogger
import dev.pellet.server.PelletBuilder.httpRouter
import dev.pellet.server.PelletBuilder.pelletServer
import dev.pellet.server.PelletConnector
import dev.pellet.server.responder.http.PelletHTTPRouteContext
import dev.pellet.server.routing.http.HTTPRouteResponse
import kotlinx.coroutines.runBlocking

object Demo

val logger = pelletLogger<Demo>()

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
                get("/v1/hello") {
                    HTTPRouteResponse.Builder()
                        .noContent()
                        .header("X-Hello", "World")
                        .build()
                }
            }
        }
    }
    pellet.start().join()
}

private suspend fun handleRequest(
    context: PelletHTTPRouteContext
): HTTPRouteResponse {
    logger.debug("got request: ${context.rawMessage}")

    return HTTPRouteResponse.Builder()
        .noContent()
        .header("X-Hello", "World")
        .build()
}
