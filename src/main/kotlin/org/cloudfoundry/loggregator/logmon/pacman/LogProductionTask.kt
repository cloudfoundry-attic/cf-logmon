package org.cloudfoundry.loggregator.logmon.pacman

import org.cloudfoundry.loggregator.logmon.logs.LogProducer
import java.time.Instant
import java.util.function.Supplier

class LogProductionTask(val logProducer: LogProducer, val numPellets: Int) : Supplier<Unit> {
    override fun get() {
        repeat(numPellets) { _ ->
            logProducer.produce()
        }
        println("${Instant.now()}: Production complete")
    }
}
