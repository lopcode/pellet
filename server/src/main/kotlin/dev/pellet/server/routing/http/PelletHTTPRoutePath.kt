package dev.pellet.server.routing.http

import dev.pellet.server.routing.RouteVariableDescriptor

public data class PelletHTTPRoutePath(
    internal val components: List<Component>
) {

    companion object {

        fun parse(rawPath: String): PelletHTTPRoutePath {
            return Builder()
                .addComponents(rawPath)
                .build()
        }
    }

    val path: String
    init {
        val joinedComponents = components.joinToString("/") {
            when (it) {
                is Component.Plain -> it.string
                is Component.Variable -> "{${it.name}}"
            }
        }
        path = "/$joinedComponents"
    }

    override fun toString(): String {
        return path
    }

    sealed class Component {

        data class Plain(val string: String) : Component()
        data class Variable(val name: String) : Component()
    }

    public class Builder {

        private var components = mutableListOf<Component>()

        fun addComponents(string: String): Builder {
            components += string
                .split("/")
                .mapNotNull {
                    val trimmedString = it
                        .removePrefix("/")
                        .removeSuffix("/")
                    if (trimmedString.isEmpty()) {
                        return@mapNotNull null
                    }
                    Component.Plain(trimmedString)
                }
            return this
        }

        fun addVariable(variableName: String): Builder {
            components += Component.Variable(variableName)
            return this
        }

        fun addVariable(descriptor: RouteVariableDescriptor<*>): Builder {
            components += Component.Variable(descriptor.name)
            return this
        }

        fun build(): PelletHTTPRoutePath {
            return PelletHTTPRoutePath(
                components
            )
        }
    }
}
