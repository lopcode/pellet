package dev.pellet.server.codec.http

object HTTPCharacters {

    val CHARS = (0..127)
        .map {
            it.toChar()
        }
        .toSet()
    val LOALPHA_CHARS = ('a'..'z').toSet()
    val UPALPHA_CHARS = ('A'..'Z').toSet()
    val ALPHA_CHARS = LOALPHA_CHARS + UPALPHA_CHARS
    val DIGIT_CHARS = ('0'..'9').toSet()
    val CONTROL_CHARS = (0..31).map { it.toChar() }.toSet() + setOf(127.toChar())

    const val CARRIAGE_RETURN = 13.toChar()
    const val LINE_FEED = 10.toChar()
    const val SPACE = 32.toChar()
    const val HORIZONTAL_TAB = 9.toChar()
    const val QUOTE = 34.toChar()
    const val AMPERSAND = 38.toChar()
    const val ASTERISK = 42.toChar()
    const val COLON = 58.toChar()
    const val SEMICOLON = 59.toChar()
    const val EQUALS = 61.toChar()
    const val COMMA = 44.toChar()
    const val FORWARD_SLASH = 47.toChar()

    val LWS_CHARS = setOf(SPACE, HORIZONTAL_TAB)

    val BYTES = CHARS.map { it.code.toByte() }.toSet()
    val LWS_BYTES = LWS_CHARS.map { it.code.toByte() }.toSet()
    const val CARRIAGE_RETURN_BYTE = CARRIAGE_RETURN.code.toByte()
    const val LINE_FEED_BYTE = LINE_FEED.code.toByte()
    const val SPACE_BYTE = SPACE.code.toByte()
    const val HORIZONTAL_TAB_BYTE = HORIZONTAL_TAB.code.toByte()
    const val QUOTE_BYTE = QUOTE.code.toByte()
    const val COLON_BYTE = COLON.code.toByte()
    const val COMMA_BYTE = COMMA.code.toByte()
}
