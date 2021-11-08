package dev.skye.pellet

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousChannel
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resumeWithException

// Inspired by https://github.com/Kotlin/kotlinx.coroutines/blob/87eaba8a287285d4c47f84c91df7671fcb58271f/integration/kotlinx-coroutines-nio/src/Nio.kt

suspend fun AsynchronousServerSocketChannel.awaitAccept(): AsynchronousSocketChannel {
    return suspendCancellableCoroutine { continuation ->
        this.accept(continuation, anyAsyncContinuationHandler())
        closeOnCancellation(continuation)
    }
}

suspend fun AsynchronousSocketChannel.awaitRead(
    buffer: ByteBuffer
): Int {
    return suspendCancellableCoroutine { continuation ->
        this.read(buffer, continuation, anyAsyncContinuationHandler())
        closeOnCancellation(continuation)
    }
}

suspend fun AsynchronousSocketChannel.awaitWrite(
    buffer: ByteBuffer
): Int {
    return suspendCancellableCoroutine { continuation ->
        this.write(buffer, continuation, anyAsyncContinuationHandler())
        closeOnCancellation(continuation)
    }
}

private fun AsynchronousChannel.closeOnCancellation(
    continuation: CancellableContinuation<*>
) {
    continuation.invokeOnCancellation {
        // intentionally ignore exceptions on close
        runCatching {
            close()
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> anyAsyncContinuationHandler(): CompletionHandler<T, CancellableContinuation<T>> =
    AnyAsyncContinuationHandler as CompletionHandler<T, CancellableContinuation<T>>

object AnyAsyncContinuationHandler : CompletionHandler<Any, CancellableContinuation<Any>> {

    override fun completed(
        result: Any,
        attachment: CancellableContinuation<Any>
    ) {
        attachment.resume(result, null)
    }

    override fun failed(
        exception: Throwable,
        attachment: CancellableContinuation<Any>
    ) {
        if (exception is AsynchronousCloseException && attachment.isCancelled) {
            return
        }
        attachment.resumeWithException(exception)
    }
}
