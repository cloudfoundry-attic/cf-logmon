package org.cloudfoundry.loggregator.logmon.pacman

import org.cloudfoundry.loggregator.logmon.logs.LogConsumer
import org.cloudfoundry.loggregator.logmon.logs.LogProducer
import org.springframework.boot.actuate.metrics.repository.MetricRepository
import reactor.core.publisher.Mono
import java.time.Duration

open class Pacman(
    val logProducer: LogProducer,
    val logConsumer: LogConsumer,
    val metricRepository: MetricRepository,
    val numPellets: Int,
    val productionDelayMillis: Long
) {
    fun begin(): Mono<Long> {
        val productionTask = LogProductionTask(logProducer, metricRepository, numPellets).get()
            .delaySubscription(Duration.ofMillis(productionDelayMillis))
            .publish().autoConnect()
            .next()
            .subscribe()

        val consumptionComplete = Mono.defer { Mono.just(LogConsumptionTask(logConsumer, productionTask).get()) }
            .log(LogConsumptionTask::class.java.name)

        return consumptionComplete.map { pelletsConsumed ->
            if (pelletsConsumed < numPellets) {
                throw notEnoughException(pelletsConsumed)
            }
            pelletsConsumed
        }
    }

    private fun notEnoughException(actual: Long) =
        PacmanBedTimeException("Got less than the requisite number of pellets: $actual < $numPellets")
}
