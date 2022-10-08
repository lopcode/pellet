package dev.pellet.demo

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty, port = 8090) {
        routing {
            get("/") {
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }.start(wait = true)
}
