package org.cloudfoundry.rivendell.pacman

import org.cloudfoundry.rivendell.logs.LogConsumer
import org.cloudfoundry.rivendell.logs.LogProducer
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

open class Pacman(
        val logProducer: LogProducer,
        val logConsumer: LogConsumer,
        val numPellets: Int,
        val executor: ExecutorService
) {
    var pelletsConsumed = 0L

    fun begin() {
        val logProductionTask = LogProductionTask(logProducer, numPellets)
        val productionComplete = Mono.fromSupplier(logProductionTask)

        val logConsumptionTask = LogConsumptionTask(logConsumer, productionComplete)
        val consumptionFuture = CompletableFuture.supplyAsync(logConsumptionTask, executor)

        val productionFuture = CompletableFuture.supplyAsync(logProductionTask, executor)

        productionFuture.get()
        pelletsConsumed = consumptionFuture.get()
        if (pelletsConsumed < numPellets) {
            productionFuture.cancel(true)
            consumptionFuture.cancel(true)
            executor.shutdownNow()
            throw PacmanBedTimeException("Got less than the requisite number of pellets: $pelletsConsumed < $numPellets")
        }

        executor.shutdownNow()
    }
}
