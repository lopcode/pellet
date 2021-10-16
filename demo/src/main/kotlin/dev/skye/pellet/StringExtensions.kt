package dev.skye.pellet

fun String.trimLWS(): String {
    return this.trim { it in HTTPCharacters.LWS_CHARS }
}
