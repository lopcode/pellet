package dev.pellet.responder.http

import dev.pellet.PelletClient

class PelletHTTPResponder(
    private val client: PelletClient
) {

    suspend fun writeNoContent() {
        val noContent = "HTTP/1.1 204 No Content\r\n\r\n"
        val bytes = Charsets.US_ASCII.encode(noContent)
        client.write(bytes)
    }

    suspend fun writeNotFound() {
        val notFound = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"
        val bytes = Charsets.US_ASCII.encode(notFound)
        client.write(bytes)
    }
}
