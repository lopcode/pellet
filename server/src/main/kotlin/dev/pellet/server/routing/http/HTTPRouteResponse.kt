package dev.pellet.server.routing.http

import dev.pellet.server.buffer.PelletBuffer
import dev.pellet.server.codec.http.ContentType
import dev.pellet.server.codec.http.ContentTypeSerialiser
import dev.pellet.server.codec.http.ContentTypes
import dev.pellet.server.codec.http.HTTPEntity
import dev.pellet.server.codec.http.HTTPHeader
import dev.pellet.server.codec.http.HTTPHeaderConstants
import dev.pellet.server.codec.http.HTTPHeaders
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.charset.Charset

data class HTTPRouteResponse(
    val statusCode: Int,
    val headers: HTTPHeaders,
    val entity: HTTPEntity
) {

    class Builder {

        private var statusCode: Int = 0
        private val headers = HTTPHeaders()
        private var entity: HTTPEntity = HTTPEntity.NoContent

        fun statusCode(code: Int): Builder {
            statusCode = code
            return this
        }

        fun header(name: String, value: String): Builder {
            headers.add(
                HTTPHeader(name, value)
            )
            return this
        }

        fun noContent(): Builder {
            statusCode = 204
            setNoContent()
            return this
        }

        fun notFound(): Builder {
            statusCode = 404
            setNoContent()
            return this
        }

        fun internalServerError(): Builder {
            statusCode = 500
            setNoContent()
            return this
        }

        fun badRequest(): Builder {
            statusCode = 400
            setNoContent()
            return this
        }

        fun entity(entity: HTTPEntity): Builder {
            this.entity = entity
            return this
        }

        fun entity(
            byteBuffer: ByteBuffer,
            rawContentType: String
        ): Builder {
            this.entity = HTTPEntity.Content(PelletBuffer(byteBuffer))
            this.headers[HTTPHeaderConstants.contentType] = rawContentType
            return this
        }

        fun entity(
            entity: String,
            contentType: ContentType
        ): Builder {
            val charset = contentType.charset() ?: Charsets.UTF_8
            val byteBuffer = charset.encode(entity)
            val contentTypeHeaderValue = ContentTypeSerialiser.serialise(contentType)
            return this.entity(byteBuffer, contentTypeHeaderValue)
        }

        fun entity(
            entity: String,
            rawContentType: String,
            charset: Charset = Charsets.UTF_8
        ): Builder {
            val byteBuffer = charset.encode(entity)
            return this.entity(byteBuffer, rawContentType)
        }

        inline fun <reified T : Any> jsonEntity(
            encoder: Json,
            value: T
        ): Builder {
            val encodedResponse = encoder.encodeToString(value)
            val contentType = ContentTypes.Application.JSON
            this.entity(encodedResponse, contentType)
            return this
        }

        inline fun <reified T : Any> jsonEntity(
            encoder: Json,
            customSerializer: SerializationStrategy<T>,
            value: T
        ): Builder {
            val encodedResponse = encoder.encodeToString(customSerializer, value)
            val contentType = ContentTypes.Application.JSON
            this.entity(encodedResponse, contentType)
            return this
        }

        // todo: what to do with failure to build? Result<>?
        fun build(): HTTPRouteResponse {
            val statusCode = this.statusCode

            val response = HTTPRouteResponse(
                statusCode = statusCode,
                headers = headers,
                entity = entity
            )
            return response
        }

        private fun setNoContent() {
            entity = HTTPEntity.NoContent
            this.headers -= HTTPHeaderConstants.contentType
            this.headers -= HTTPHeaderConstants.contentLength
        }
    }
}
