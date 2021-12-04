package dev.pellet.codec.http

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

    val CARRIAGE_RETURN = 13.toChar()
    val LINE_FEED = 10.toChar()
    val SPACE = 32.toChar()
    val HORIZONTAL_TAB = 9.toChar()
    val QUOTE = 34.toChar()
    val COLON = 58.toChar()

    val LWS_CHARS = setOf(SPACE, HORIZONTAL_TAB)

    val BYTES = CHARS.map { it.code.toByte() }.toSet()
    val LWS_BYTES = LWS_CHARS.map { it.code.toByte() }.toSet()
    val CARRIAGE_RETURN_BYTE = CARRIAGE_RETURN.code.toByte()
    val LINE_FEED_BYTE = LINE_FEED.code.toByte()
    val SPACE_BYTE = SPACE.code.toByte()
    val HORIZONTAL_TAB_BYTE = HORIZONTAL_TAB.code.toByte()
    val QUOTE_BYTE = QUOTE.code.toByte()
}
