package dev.pellet.server.codec.http

import java.util.Locale

data class HTTPHeaders(
    private val storage: MutableMap<String, MutableList<HTTPHeader>> = mutableMapOf()
) {

    operator fun set(headerName: String, value: String): HTTPHeaders {
        add(
            HTTPHeader(
                rawName = headerName,
                rawValue = value
            )
        )
        return this
    }

    operator fun get(headerName: String): HTTPHeader? {
        return getSingleOrNull(headerName)
    }

    operator fun minusAssign(headerName: String) {
        remove(headerName)
    }

    fun getSingleOrNull(headerName: String): HTTPHeader? {
        val headers = getAll(headerName)
        if (headers.size != 1) {
            return null
        }

        return headers.firstOrNull()
    }

    fun getAll(headerName: String): List<HTTPHeader> {
        val normalisedName = normaliseName(headerName)
        return storage[normalisedName] ?: listOf()
    }

    fun add(header: HTTPHeader): HTTPHeaders {
        val normalisedName = normaliseName(header.rawName)
        val list = storage[normalisedName] ?: mutableListOf()
        list.add(header)
        storage[normalisedName] = list
        return this
    }

    fun add(rawName: String, rawValue: String): HTTPHeaders {
        val header = HTTPHeader(rawName, rawValue)
        return add(header)
    }

    fun remove(headerName: String): HTTPHeaders {
        val normalisedName = normaliseName(headerName)
        storage -= normalisedName
        return this
    }

    private fun normaliseName(headerName: String): String {
        return headerName.lowercase(Locale.ENGLISH)
    }

    override fun toString(): String {
        return storage.toString()
    }

    fun forEach(lambda: (String, List<HTTPHeader>) -> Unit) {
        storage.forEach {
            lambda(it.key, it.value)
        }
    }
}
