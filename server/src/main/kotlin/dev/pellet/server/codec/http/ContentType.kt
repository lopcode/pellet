package dev.pellet.server.codec.http

import dev.pellet.server.codec.mime.MediaType
import dev.pellet.server.codec.mime.MediaTypeParser

typealias ContentType = MediaType
typealias ContentTypeParser = MediaTypeParser

object ContentTypes {

    object Text {

        val Plain = ContentType("text", "plain")
    }

    object Application {

        val JSON = ContentType(type = "application", subtype = "json")
    }
}

fun ContentType.matches(other: ContentType): Boolean {
    if (this.type == "*") {
        return true
    }

    if (!this.type.equals(other.type, ignoreCase = true)) {
        return false
    }

    if (this.subtype == "*") {
        return true
    }

    if (!this.subtype.equals(other.subtype, ignoreCase = true)) {
        return false
    }

    // todo: parameter wildcards?
    return other.parameters.containsAll(this.parameters)
}
