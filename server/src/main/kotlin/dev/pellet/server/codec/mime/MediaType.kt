package dev.pellet.server.codec.mime

import java.nio.charset.Charset

data class MediaType(
    val type: String,
    val subtype: String,
    val parameters: List<Pair<String, String>> = listOf()
) {

    /**
     * Attempts to find the first charset as specified by [parameters]
     */
    fun charset(): Charset? {
        val charsetValue = parameters
            .firstOrNull {
                it.first.equals("charset", ignoreCase = true)
            }
            ?.first
            ?: return null
        return runCatching {
            Charset.forName(charsetValue)
        }.getOrNull()
    }
}
