package org.cloudfoundry.loggregator.logmon.pacman

import org.cloudfoundry.loggregator.logmon.logs.LogConsumer
import org.cloudfoundry.loggregator.logmon.logs.LogProducer
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.function.Supplier

open class Pacman(
    val logProducer: LogProducer,
    val logConsumer: LogConsumer,
    val numPellets: Int
) {
    fun begin(): Mono<Long> {
        val logProductionTask = LogProductionTask(logProducer, numPellets)
        val productionTask = queueUpTask(logProductionTask, delaySeconds = 2)

        val logConsumptionTask = LogConsumptionTask(logConsumer, productionTask)
        val consumptionComplete = queueUpTask(logConsumptionTask, delaySeconds = 0)

        return consumptionComplete.and(productionTask).map { tuple ->
            val pelletsConsumed = tuple.t1
            if (pelletsConsumed < numPellets) {
                throw notEnoughException(pelletsConsumed)
            }
            pelletsConsumed
        }
    }

    private fun <T> queueUpTask(task: Supplier<T>, delaySeconds: Long): Mono<T> {
        return Flux.fromIterable(listOf(task))
            .delayElements(Duration.ofSeconds(delaySeconds))
            .log()
            .map { t -> t.get() }
            .publish().autoConnect()
            .next()
    }

    private fun notEnoughException(actual: Long) =
        PacmanBedTimeException("Got less than the requisite number of pellets: $actual < $numPellets")
}
