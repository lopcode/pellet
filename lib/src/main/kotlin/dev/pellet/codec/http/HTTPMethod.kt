package dev.pellet.codec.http

import java.util.Locale

// Cannot be contained within HTTPMethod companion object due to initialization cycles
internal val defaultMethods = listOf(
    HTTPMethod.Get,
    HTTPMethod.Post,
    HTTPMethod.Put,
    HTTPMethod.Patch,
    HTTPMethod.Delete,
    HTTPMethod.Head,
    HTTPMethod.Connect,
    HTTPMethod.Options,
    HTTPMethod.Trace
)

sealed class HTTPMethod(
    internal val rawMethod: String
) {

    override fun toString(): String {
        return rawMethod.uppercase(Locale.US)
    }

    object Get : HTTPMethod("GET")
    object Post : HTTPMethod("POST")
    object Put : HTTPMethod("PUT")
    object Patch : HTTPMethod("PATCH")
    object Delete : HTTPMethod("DELETE")
    object Head : HTTPMethod("HEAD")
    object Connect : HTTPMethod("CONNECT")
    object Options : HTTPMethod("OPTIONS")
    object Trace : HTTPMethod("TRACE")
    data class Custom(val method: String) : HTTPMethod(method)
}
