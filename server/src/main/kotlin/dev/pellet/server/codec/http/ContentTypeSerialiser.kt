package dev.pellet.server.codec.http

object ContentTypeSerialiser {

    fun serialise(contentType: ContentType): String {
        val builder = StringBuilder()
        builder.append("${contentType.type}/${contentType.subtype}")
        if (contentType.parameters.isEmpty()) {
            return builder.toString()
        }

        val parametersSection = contentType.parameters.joinToString(
            separator = HTTPCharacters.SEMICOLON.toString(),
            prefix = HTTPCharacters.SEMICOLON.toString()
        ) {
            "${it.first}=\"${it.second}\""
        }
        builder.append(parametersSection)
        return builder.toString()
    }
}
