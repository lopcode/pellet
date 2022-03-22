package dev.pellet.server.responder.http

import dev.pellet.server.PelletServerClient
import dev.pellet.server.codec.http.HTTPRequestMessage
import dev.pellet.server.routing.RouteVariableDescriptor

data class PelletHTTPRouteContext(
    val rawMessage: HTTPRequestMessage,
    internal val client: PelletServerClient,
    internal val pathValueMap: Map<String, String>
) {

    fun <T : Any> pathParameter(
        descriptor: RouteVariableDescriptor<T>
    ): Result<T> {
        val rawValue = pathValueMap[descriptor.name]
            ?: return Result.failure(
                RuntimeException("no such path parameter found")
            )
        // todo: wrap deserialiser errors?
        return runCatching {
            descriptor.deserialiser.invoke(rawValue)
        }
    }

    fun pathParameter(
        name: String
    ): Result<String> {
        val rawValue = pathValueMap[name]
            ?: return Result.failure(
                RuntimeException("no such path parameter found")
            )
        return Result.success(rawValue)
    }
}
