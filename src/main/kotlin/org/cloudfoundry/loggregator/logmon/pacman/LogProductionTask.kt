package org.cloudfoundry.loggregator.logmon.pacman

import org.cloudfoundry.loggregator.logmon.logs.LogProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Supplier

class LogProductionTask(val logProducer: LogProducer, val numPellets: Int) : Supplier<Unit> {
    companion object {
        val log: Logger = LoggerFactory.getLogger(LogProductionTask::class.java)
    }

    override fun get() {
        log.info("Production starting")

        repeat(numPellets) { _ ->
            logProducer.produce()
        }

        log.info("Production complete")
    }
}
