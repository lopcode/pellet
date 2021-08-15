package dev.skye.pellet

object Demo

fun main() {
    val library = Library()
    val logger = logger<Demo>()
    val result = library.someLibraryMethod()

    logger.info("library method result: $result")
}
