package dev.pellet.responder.http

import dev.pellet.codec.http.HTTPResponseMessage

interface PelletHTTPResponding {

    suspend fun respond(message: HTTPResponseMessage): Result<Unit>
}
