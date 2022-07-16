package dev.pellet.server.codec.mime

import java.nio.charset.Charset

data class MediaType(
    val type: String,
    val subtype: String,
    val parameters: List<Pair<String, String>> = listOf()
) {

    constructor(
        type: String,
        subtype: String,
        vararg parameters: Pair<String, String>
    ) : this(type, subtype, parameters.toList())

    /**
     * Attempts to find the first charset as specified by [parameters]
     */
    fun charsetOrNull(): Charset? {
        val charsetValue = parameters
            .firstOrNull {
                it.first.equals("charset", ignoreCase = true)
            }
            ?.second
            ?: return null
        return runCatching {
            Charset.forName(charsetValue)
        }.getOrNull()
    }
}
