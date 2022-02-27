package dev.pellet.server.metrics

import java.time.Duration

interface PelletTiming {

    fun reset()
    fun markAndReset(): Duration
}
