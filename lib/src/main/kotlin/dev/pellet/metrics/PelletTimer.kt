package dev.pellet.metrics

import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

class PelletTimer : PelletTiming {

    private val mark = AtomicLong(System.nanoTime())

    override fun reset() {
        val now = System.nanoTime()
        mark.set(now)
    }

    override fun markAndReset(): Duration {
        val now = System.nanoTime()
        val previousMark = mark.get()
        val difference = now - previousMark
        mark.set(now)
        return Duration.ofNanos(difference)
    }
}
