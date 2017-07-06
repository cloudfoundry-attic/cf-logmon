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
        val productionTask = queueUpDelayedTask(LogProductionTask(logProducer, numPellets), delayMillis = 2500)
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

    private fun <T> queueUpDelayedTask(task: Supplier<T>, delayMillis: Long): Mono<T> {
        return Flux.from<T>({
            task.get()
            it.onComplete()
        })
            .delaySubscription(Duration.ofMillis(delayMillis))
            .log(task::class.java.name)
            .publish().autoConnect()
            .next()
    }

    private fun notEnoughException(actual: Long) =
        PacmanBedTimeException("Got less than the requisite number of pellets: $actual < $numPellets")
}
