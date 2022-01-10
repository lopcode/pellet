package dev.pellet.metrics

import java.time.Duration

interface PelletTiming {

    fun reset()
    fun markAndReset(): Duration
}
