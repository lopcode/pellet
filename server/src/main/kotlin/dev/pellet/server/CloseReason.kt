package dev.pellet.server

sealed class CloseReason {

    data object ClientInitiated : CloseReason()
    data object ServerInitiated : CloseReason()
    data class ServerException(val throwable: Throwable) : CloseReason()
}
