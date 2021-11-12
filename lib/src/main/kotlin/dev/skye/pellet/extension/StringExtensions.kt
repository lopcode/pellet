package dev.skye.pellet.extension

import dev.skye.pellet.codec.http.HTTPCharacters

fun String.trimLWS(): String {
    return this.trim { it in HTTPCharacters.LWS_CHARS }
}
