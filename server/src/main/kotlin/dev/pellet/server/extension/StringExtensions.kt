package dev.pellet.server.extension

import dev.pellet.server.codec.http.HTTPCharacters

fun String.trimLWS(): String {
    return this.trim { it in HTTPCharacters.LWS_CHARS }
}
