package org.cloudfoundry.loggregator.logmon.logs

import reactor.core.publisher.Mono

interface LogConsumer {
    fun consume(productionCompleteNotifier: Mono<Unit>): Mono<Long>
}
