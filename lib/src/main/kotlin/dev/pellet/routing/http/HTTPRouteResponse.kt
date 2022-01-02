package dev.pellet.routing.http

import dev.pellet.codec.http.HTTPEntity
import dev.pellet.codec.http.HTTPHeader
import dev.pellet.codec.http.HTTPHeaders

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
            entity = HTTPEntity.NoContent
            return this
        }

        fun notFound(): Builder {
            statusCode = 404
            entity = HTTPEntity.NoContent
            return this
        }

        fun internalServerError(): Builder {
            statusCode = 500
            entity = HTTPEntity.NoContent
            return this
        }

        fun entity(entity: HTTPEntity): Builder {
            this.entity = entity
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
    }
}
