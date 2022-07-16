package dev.pellet.server.codec.http.query

import dev.pellet.server.codec.http.HTTPCharacters
import java.net.URLDecoder

object QueryParser {

    /**
     * Parses an encoded query string (of the format "a=b&c") and returns decoded keys/values
     */
    fun parseEncodedQuery(string: String): Result<QueryParameters> {
        if (string.isEmpty()) {
            return Result.success(QueryParameters(mapOf()))
        }

        val queryMap = mutableMapOf<String, MutableList<String?>>()
        var currentIndex = 0
        while (currentIndex < string.length) {
            val nextAndIndex = string.indexOf(
                HTTPCharacters.AMPERSAND,
                startIndex = currentIndex
            )
            if (nextAndIndex == 0) {
                return Result.failure(
                    IllegalArgumentException("unexpected & in url")
                )
            } else if (nextAndIndex > 0) {
                val segment = string.substring(currentIndex, nextAndIndex)
                val (name, value) = parseAndDecodeSegment(segment).getOrElse {
                    return Result.failure(it)
                }
                storeSegment(name, value, queryMap)
                currentIndex = nextAndIndex + 1
            } else {
                // no more &, last segment
                val segment = string.substring(currentIndex)
                val (name, value) = parseAndDecodeSegment(segment).getOrElse {
                    return Result.failure(it)
                }
                storeSegment(name, value, queryMap)
                currentIndex = string.length
            }
        }

        return Result.success(
            QueryParameters(queryMap)
        )
    }

    private fun storeSegment(
        name: String,
        value: String?,
        queryMap: MutableMap<String, MutableList<String?>>
    ) {
        val newValues = queryMap[name]?.let {
            it.add(value)
            it
        } ?: mutableListOf(value)
        queryMap[name] = newValues
    }

    private fun parseAndDecodeSegment(encodedSegment: String): Result<Pair<String, String?>> {
        val firstEqualsIndex = encodedSegment.indexOf(HTTPCharacters.EQUALS)
        if (firstEqualsIndex == 0) {
            return Result.failure(
                IllegalArgumentException("unexpected = in url")
            )
        } else if (firstEqualsIndex < 0) {
            // no value, just a name
            val name = decode(encodedSegment).getOrElse {
                return Result.failure(it)
            }
            return Result.success(name to null)
        }

        val encodedName = encodedSegment.substring(0, firstEqualsIndex)
        val name = decode(encodedName).getOrElse {
            return Result.failure(it)
        }
        if (firstEqualsIndex >= encodedSegment.length - 1) {
            // no set value, empty string
            return Result.success(name to "")
        }

        val encodedValue = encodedSegment.substring(firstEqualsIndex + 1)
        val value = decode(encodedValue).getOrElse {
            return Result.failure(it)
        }
        return Result.success(name to value)
    }

    private fun decode(encodedSegment: String): Result<String> {
        return runCatching {
            URLDecoder.decode(encodedSegment, Charsets.UTF_8)
        }
    }
}
