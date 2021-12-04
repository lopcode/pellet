package dev.pellet

fun main() {
    /*
    val server = pelletServer {
        httpConnector {
            server = "0.0.0.0"
            port = 8081
            routes = configureRoutes()
        }
        httpConnector {
            server = "localhost"
            port = 8082
            routes {
                get("/healthcheck") { context, responder ->
                    responder.writeNoContent()
                }
            }
        }
    }
    fun Connector.configureRoutes() {
        routes {
            get("/v1/hello") { context, responder ->
                responder.writePlaintext("hello, world!")
            }
        }
    }
    server.start()
    */
}
