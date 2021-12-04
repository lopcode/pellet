package dev.pellet.responder.http

data class HTTPRoute(
    val path: String,
    val action: suspend (PelletHTTPContext, PelletHTTPResponder) -> Unit
)
