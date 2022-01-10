package dev.pellet

import dev.pellet.routing.http.HTTPRouting

sealed class PelletConnector {

    data class HTTP(
        val endpoint: Endpoint,
        val router: HTTPRouting
    ) : PelletConnector() {

        override fun toString(): String {
            return "HTTP(hostname=${endpoint.hostname}, port=${endpoint.port}, router=$router)"
        }
    }

    data class Endpoint(
        val hostname: String,
        val port: Int
    )
}
