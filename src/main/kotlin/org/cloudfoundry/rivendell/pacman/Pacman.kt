package org.cloudfoundry.rivendell.pacman

import org.cloudfoundry.rivendell.logs.LogConsumer
import org.cloudfoundry.rivendell.logs.LogProducer
import reactor.core.publisher.Mono
import reactor.core.publisher.UnicastProcessor
import java.time.Duration

open class Pacman(
    val logProducer: LogProducer,
    val logConsumer: LogConsumer,
    val numPellets: Int
) {
    var pelletsConsumed = 0L

    fun begin() {
        val logProductionTask = LogProductionTask(logProducer, numPellets)
        val processor = UnicastProcessor.create<Unit>()
        val productionTask = processor.publish().autoConnect().next()
            .delayElement(Duration.ofSeconds(2))
            .doOnNext { println("Starting..."); logProductionTask.get() }
            .ignoreElement()

        val logConsumptionTask = LogConsumptionTask(logConsumer, productionTask)
        val consumptionComplete = Mono.fromSupplier(logConsumptionTask)

        processor.onNext({}.invoke())
        consumptionComplete.subscribe {
            productionTask.block()
            pelletsConsumed = it
        }
        if (pelletsConsumed < numPellets) {
            throw PacmanBedTimeException("Got less than the requisite number of pellets: $pelletsConsumed < $numPellets")
        }
    }
}
