package dev.pellet.extension

import dev.pellet.codec.http.HTTPCharacters

fun String.trimLWS(): String {
    return this.trim { it in HTTPCharacters.LWS_CHARS }
}
