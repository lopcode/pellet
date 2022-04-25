package dev.pellet.server.extension

import dev.pellet.server.codec.http.HTTPCharacters

fun String.trimLWS(): String {
    return this.trim { it in HTTPCharacters.LWS_CHARS }
}

fun String.trimEndLWS(): String {
    return this.trimEnd { it in HTTPCharacters.LWS_CHARS }
}

fun CharSequence.isEmptyOrLWS(): Boolean {
    if (this.isEmpty()) {
        return true
    }

    return indices.all {
        this[it] in HTTPCharacters.LWS_CHARS
    }
}

// Removes a character if present at both start and end of string
fun String.removeSurrounding(character: Char): String {
    if (!this.startsWith(character)) {
        return this
    }
    if (!this.endsWith(character)) {
        return this
    }
    return this.substring(1, this.length - 1)
}
