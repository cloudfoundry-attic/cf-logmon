package org.cloudfoundry.loggregator.logmon.pacman

import org.cloudfoundry.loggregator.logmon.logs.LogProducer
import org.cloudfoundry.loggregator.logmon.statistics.LOG_WRITE_TIME_MILLIS
import org.cloudfoundry.loggregator.logmon.statistics.setImmediate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.metrics.repository.MetricRepository
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.function.Supplier

class LogProductionTask(val logProducer: LogProducer, val metricRepository: MetricRepository, val numPellets: Int) : Supplier<Flux<Unit>> {
    companion object {
        val log: Logger = LoggerFactory.getLogger(LogProductionTask::class.java)
    }

    /**
     * Generate logs at given intervals.
     * For pellet counts up to 1000, we can generate a reasonable delay between log production events.
     * When the pellet count goes over 1000, the JVM can't guarantee sub-ms precision, so we batch the
     * pellets and generate batches of logs on each tick for 1000 ms.
     *
     * With 2 pellets:
     *
     * Time: 0------------500------------1000
     *
     * Logs: 1L-----------1L-------------0L -- Complete
     *
     *
     * With 2001 pellets:
     *
     * Time: 0---1---2---.....-----------1000
     *
     * Logs: 3L--2L--2L--.....-----------2L -- Complete
     */
    override fun get(): Flux<Unit> {
        log.info("Production starting")

        val flux = if (numPellets <= 1000) {
            val writeRate = 1000L / numPellets

            Flux.interval(Duration.ofMillis(0), Duration.ofMillis(writeRate))
                .doOnNext({ logProducer.produce() })
                .take(numPellets.toLong())
        } else {
            val buckets = (0..999).associate { Pair(it, numPellets / 1000 + if (numPellets % 1000 > it) 1 else 0) }

            Flux.fromIterable(buckets.values)
                .doOnNext({ repeat(it) { logProducer.produce() } })
                .delayElements(Duration.ofMillis(1))
        }

        return flux
            .map { }
            .doOnComplete {
                metricRepository.setImmediate(LOG_WRITE_TIME_MILLIS, 1000)
                log.info("Production complete")
            }
    }
}
