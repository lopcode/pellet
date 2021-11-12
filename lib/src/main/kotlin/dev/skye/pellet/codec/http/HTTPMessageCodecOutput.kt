package dev.skye.pellet.codec.http

import dev.skye.pellet.PelletClient
import dev.skye.pellet.PelletContext
import dev.skye.pellet.PelletResponder
import dev.skye.pellet.codec.CodecOutput

internal class HTTPMessageCodecOutput(
    private val client: PelletClient,
    private val action: suspend (PelletContext, PelletResponder) -> Unit
) : CodecOutput<HTTPRequestMessage> {

    override suspend fun output(thing: HTTPRequestMessage) {
        val request = PelletContext(thing, client)
        val responder = PelletResponder(client)
        action(request, responder)
    }
}
