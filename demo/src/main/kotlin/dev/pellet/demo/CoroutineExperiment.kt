package dev.pellet.demo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KFunction2

object CoroutineExperiment

fun main() = runBlocking {
    val counter = AtomicInteger(0)
    val runtimeSeconds = 30
    measure("coroutine per count", ::launchPerCount, runtimeSeconds, counter)
    measure("coroutine per processor", ::launchPerProcessor, runtimeSeconds, counter)
}

private fun CoroutineScope.measure(
    name: String,
    launcher: KFunction2<Int, AtomicInteger, Unit>,
    runtimeSeconds: Int,
    counter: AtomicInteger
) {
    println("launching experiment: $name")
    launcher(runtimeSeconds, counter)
    val counted = counter.get()
    val cps = counted / runtimeSeconds.toDouble()
    println("$name: incremented counter $counted times in $runtimeSeconds seconds ($cps per second)")
}

private fun CoroutineScope.launchPerCount(
    runtimeSeconds: Int,
    counter: AtomicInteger
) {
    val start = Instant.now()
    while (true) {
        val duration = Duration.between(start, Instant.now())
        if (duration.toSeconds() > runtimeSeconds) {
            break
        }
        launch(Dispatchers.Default) {
            counter.incrementAndGet()
        }
    }
}

private fun CoroutineScope.launchPerProcessor(
    runtimeSeconds: Int,
    counter: AtomicInteger
) {
    val start = Instant.now()
    val processors = Runtime.getRuntime().availableProcessors()
    val supervisor = SupervisorJob() + Dispatchers.Default
    (0..processors).map {
        launch(supervisor) {
            while (isActive) {
                counter.incrementAndGet()
            }
        }
    }
    runBlocking {
        while (true) {
            val duration = Duration.between(start, Instant.now())
            if (duration.toSeconds() > runtimeSeconds) {
                supervisor.cancel()
                break
            }
            delay(500)
        }
    }
}
