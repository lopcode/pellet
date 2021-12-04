package dev.pellet

import dev.pellet.responder.http.HTTPRouting

sealed class PelletConnector {

    data class HTTP(
        val endpoint: Endpoint,
        val router: HTTPRouting
    ) : PelletConnector() {

        override fun toString(): String {
            return "HTTP(hostname=${endpoint.hostname}, port=${endpoint.port})"
        }
    }

    data class Endpoint(
        val hostname: String,
        val port: Int
    )
}
