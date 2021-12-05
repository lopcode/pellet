package dev.pellet.codec.http

sealed class HTTPMethod {

    object Get : HTTPMethod()
    object Post : HTTPMethod()
    object Put : HTTPMethod()
    object Patch : HTTPMethod()
    object Delete : HTTPMethod()
    object Head : HTTPMethod()
    object Connect : HTTPMethod()
    object Options : HTTPMethod()
    object Trace : HTTPMethod()
    data class Custom(val method: String) : HTTPMethod()
}
