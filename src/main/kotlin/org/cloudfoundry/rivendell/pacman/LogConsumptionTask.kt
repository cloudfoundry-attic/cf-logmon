package org.cloudfoundry.rivendell.pacman

import org.cloudfoundry.rivendell.logs.LogConsumer
import reactor.core.publisher.Mono
import java.util.function.Supplier

class LogConsumptionTask(val logConsumer: LogConsumer, val productionCompleteNotifier: Mono<Unit>) : Supplier<Long> {
    override fun get(): Long {
        return logConsumer.consume(productionCompleteNotifier).block()
    }
}
