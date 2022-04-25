package dev.pellet

import dev.pellet.server.codec.http.query.QueryParameters
import kotlin.test.assertEquals
import kotlin.test.assertIs

@Suppress("NOTHING_TO_INLINE")
inline fun <reified T : Any> assertSuccess(
    expected: T,
    actual: Result<T>
) {
    val result = actual.getOrThrow()
    assertIs<T>(expected)
    assertEquals(expected, result)
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T : Any> assertFailure(
    expected: Throwable?,
    actual: Result<QueryParameters>
) {
    val result = actual.exceptionOrNull()
        ?: throw java.lang.AssertionError("expected a failure: $actual")
    if (expected != null) {
        assertEquals(expected, result)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T : Any> assertFailure(
    actual: Result<T>
) {
    actual.exceptionOrNull()
        ?: throw java.lang.AssertionError("expected a failure: $actual")
}

@Suppress("NOTHING_TO_INLINE")
inline fun <reified E : Throwable> assertFailureIs(
    actual: Result<*>
) {
    val throwable = actual.exceptionOrNull()
        ?: throw java.lang.AssertionError("expected a failure: $actual")

    assertIs<E>(throwable)
}
