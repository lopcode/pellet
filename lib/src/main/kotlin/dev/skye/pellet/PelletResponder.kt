package dev.skye.pellet

import java.io.IOException

class PelletResponder(
    private val client: PelletClient
) {

    suspend fun writeNoContent() {
        val noContent = "HTTP/1.1 204 No Content\r\n\r\n"
        val bytes = Charsets.US_ASCII.encode(noContent)
        try {
            client.write(bytes)
        } catch (exception: IOException) {
            // ignore
        }
    }
}
