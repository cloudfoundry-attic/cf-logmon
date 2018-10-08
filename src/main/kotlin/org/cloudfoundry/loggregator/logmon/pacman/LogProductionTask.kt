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

class LogProductionTask(val logProducer: LogProducer, val metricRepository: MetricRepository, val durationMillis: Int, val numPellets: Int) : Supplier<Flux<Unit>> {
    companion object {
        val log: Logger = LoggerFactory.getLogger(LogProductionTask::class.java)
    }

    /**
     * Generate logs at given intervals.
     * For pellet counts up to 1000, we can generate a reasonable delay between log production events.
     * When the pellet count goes over 1000, the JVM can't guarantee sub-ms precision, so we emit multiple per
     * millisecond. Any pellets that cannot be evenly distributed are front-loaded on the first M milliseconds.
     *
     * With 2 pellets:
     *
     * Time: 0------------500------------1000
     *
     * Logs: 1L-----------1L-------------0L -- Complete
     *
     *
     * With 2002 pellets:
     *
     * Time: 0---1---2---.....-----------1000
     *
     * Logs: 3L--3L--2L--.....-----------2L -- Complete
     */
    override fun get(): Flux<Unit> {
        log.info("Production starting")

        val flux = if (numPellets <= durationMillis) {
            val writeRate = durationMillis / numPellets

            Flux.interval(Duration.ofMillis(0), Duration.ofMillis(writeRate.toLong()))
                .doOnNext({ logProducer.produce() })
                .take(numPellets.toLong())
        } else {
            Flux.interval(Duration.ofMillis(0), Duration.ofMillis(1))
                .doOnNext({ repeat(numPellets / durationMillis + if (numPellets % durationMillis > it) 1 else 0) {
                        logProducer.produce();
                    }
                })
                .take(durationMillis.toLong())
        }

        return flux
            .map { }
            .doOnComplete {
                metricRepository.setImmediate(LOG_WRITE_TIME_MILLIS, durationMillis)
                log.info("Production complete")
            }
    }
}
