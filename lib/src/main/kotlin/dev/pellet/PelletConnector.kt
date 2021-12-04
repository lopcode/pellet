package dev.pellet

sealed class PelletConnector {

    data class HTTP(
        val hostname: String,
        val port: Int
    ) : PelletConnector()
}
