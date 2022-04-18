package dev.pellet.server.codec.http.query

data class QueryParameters(
    val values: Map<String, List<String?>>
) {

    operator fun get(key: String): List<String?>? {
        return values[key]
    }
}
