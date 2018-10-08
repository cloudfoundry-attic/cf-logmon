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
    val durationMillis: Int,
    val numPellets: Int,
    val productionDelayMillis: Long
) {
    fun begin(): Mono<Long> {
        val productionTask = LogProductionTask(logProducer, metricRepository, durationMillis, numPellets).get()
            .delaySubscription(Duration.ofMillis(productionDelayMillis))
            .publish().autoConnect()
            .ignoreElements()
            .subscribe()

        val consumptionTask = Mono.defer {
            Mono.just(LogConsumptionTask(logConsumer, productionTask).get())
        }.subscribe()

        return productionTask.then(consumptionTask)
            .log(LogConsumptionTask::class.java.name)
    }
}
