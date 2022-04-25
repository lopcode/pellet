package dev.pellet.server.codec.mime

import dev.pellet.server.codec.ParseException
import dev.pellet.server.codec.http.HTTPCharacters
import dev.pellet.server.extension.isEmptyOrLWS
import dev.pellet.server.extension.removeSurrounding
import dev.pellet.server.extension.trimEndLWS
import dev.pellet.server.extension.trimLWS

object MediaTypeParser {

    /**
     * Parses "media type" strings as per https://www.rfc-editor.org/rfc/inline-errata/rfc7231.html
     *
     *  Spec:
     *  media-type = type "/" subtype *( OWS ";" OWS parameter )
     *  type       = token
     *  subtype    = token

     *  The type/subtype MAY be followed by parameters in the form of
     *  name=value pairs.

     *  parameter      = token "=" ( token / quoted-string )
     */
    fun parse(string: String): Result<MediaType> {
        if (string.isEmptyOrLWS() || string.length < 3) {
            return Result.failure(
                ParseException("empty / malformed media type string")
            )
        }

        val firstSemicolonIndex = string.indexOf(HTTPCharacters.SEMICOLON)
        if (firstSemicolonIndex < 0) {
            // the entire string is the type/subtype part
            val (type, subtype) = parseTypeSubtype(string).getOrElse {
                return Result.failure(it)
            }
            return Result.success(
                MediaType(
                    type = type,
                    subtype = subtype,
                    parameters = listOf()
                )
            )
        }

        var currentIndex = 0
        val typeSubtypePart = string.substring(currentIndex, firstSemicolonIndex)
        val (type, subtype) = parseTypeSubtype(typeSubtypePart).getOrElse {
            return Result.failure(it)
        }
        currentIndex += firstSemicolonIndex + 1

        val parameters = mutableListOf<Pair<String, String>>()
        while (currentIndex < string.length) {
            val nextSemicolonIndex = string.indexOf(HTTPCharacters.SEMICOLON, currentIndex)
            if (nextSemicolonIndex <= 0) {
                // no more semicolons/parameters - parse the rest of the string
                val restOfString = string
                    .substring(currentIndex)
                    .trimLWS()
                val parameter = parseParameter(restOfString).getOrElse {
                    return Result.failure(it)
                }
                parameters += parameter
                break
            }

            val nextParameter = string
                .substring(currentIndex, nextSemicolonIndex)
                .trimLWS()
            val parameter = parseParameter(nextParameter).getOrElse {
                return Result.failure(it)
            }
            parameters += parameter
            currentIndex = nextSemicolonIndex + 1
        }

        return Result.success(
            MediaType(
                type = type,
                subtype = subtype,
                parameters = parameters
            )
        )
    }

    private fun parseTypeSubtype(string: String): Result<Pair<String, String>> {
        val firstSlashPosition = string.indexOf(HTTPCharacters.FORWARD_SLASH)
        if (firstSlashPosition <= 0 || firstSlashPosition > string.length - 1) {
            return Result.failure(
                ParseException("malformed type/subtype")
            )
        }

        val type = string.substring(0, firstSlashPosition)
        val subtype = string
            .substring(firstSlashPosition + 1)
            .trimEndLWS()
        if (type.isEmptyOrLWS() || subtype.isEmptyOrLWS()) {
            return Result.failure(
                ParseException("malformed type/subtype, empty parts")
            )
        }
        if (subtype.contains(HTTPCharacters.FORWARD_SLASH)) {
            return Result.failure(
                ParseException("malformed subtype, contains /")
            )
        }
        return Result.success(type to subtype)
    }

    private fun parseParameter(string: String): Result<Pair<String, String>> {
        val firstEqualsPosition = string.indexOf(HTTPCharacters.EQUALS)
        if (firstEqualsPosition <= 0 || firstEqualsPosition > string.length - 1) {
            return Result.failure(
                ParseException("malformed parameter")
            )
        }

        val key = string.substring(0, firstEqualsPosition)
        val value = string
            .substring(firstEqualsPosition + 1)
            .removeSurrounding(HTTPCharacters.QUOTE)
        if (key.isEmptyOrLWS() || value.isEmptyOrLWS()) {
            return Result.failure(
                ParseException("malformed parameter key or value - lws")
            )
        }
        return Result.success(key to value)
    }
}
