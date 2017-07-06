package org.cloudfoundry.loggregator.logmon.pacman

import org.cloudfoundry.loggregator.logmon.logs.LogProducer
import org.cloudfoundry.loggregator.logmon.statistics.LOG_WRITE_TIME_MILLIS
import org.cloudfoundry.loggregator.logmon.statistics.setImmediate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.metrics.repository.MetricRepository
import java.util.function.Supplier

class LogProductionTask(val logProducer: LogProducer, val metricRepository: MetricRepository, val numPellets: Int) : Supplier<Unit> {
    companion object {
        val log: Logger = LoggerFactory.getLogger(LogProductionTask::class.java)
    }

    override fun get() {
        log.info("Production starting")

        val nanoTime = time {
            repeat(numPellets) { _ ->
                logProducer.produce()
            }
        }

        metricRepository.setImmediate(LOG_WRITE_TIME_MILLIS, nanoTime / 1_000_000.0)
        log.info("Production complete")
    }
}

fun time(task: () -> Unit): Long {
    val start = System.nanoTime()
    task.invoke()
    val stop = System.nanoTime()
    return stop - start
}
