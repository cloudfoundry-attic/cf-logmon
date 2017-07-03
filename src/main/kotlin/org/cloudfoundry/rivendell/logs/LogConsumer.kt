package org.cloudfoundry.rivendell.logs

import reactor.core.publisher.Mono

interface LogConsumer {
    fun consume(productionCompleteNotifier: Mono<Unit>): Mono<Long>
}
