package dev.skye.pellet

import java.util.Locale

data class HTTPHeaders(
    private val storage: MutableMap<String, MutableList<HTTPHeader>> = mutableMapOf()
) {

    fun getSingleOrNull(headerName: String): HTTPHeader? {
        val headers = get(headerName)
        if (headers.size != 1) {
            return null
        }

        return headers.firstOrNull()
    }

    fun get(headerName: String): List<HTTPHeader> {
        val normalisedName = normaliseName(headerName)
        return storage[normalisedName] ?: listOf()
    }

    fun add(header: HTTPHeader) {
        val normalisedName = normaliseName(header.rawName)
        val list = storage[normalisedName] ?: mutableListOf()
        list.add(header)
        storage[normalisedName] = list
    }

    private fun normaliseName(headerName: String): String {
        return headerName.lowercase(Locale.ENGLISH)
    }

    override fun toString(): String {
        return storage.toString()
    }
}
