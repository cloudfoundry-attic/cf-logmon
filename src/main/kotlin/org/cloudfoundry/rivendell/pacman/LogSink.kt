package org.cloudfoundry.rivendell.pacman

import org.cloudfoundry.rivendell.cf.CfApplicationEnv
import org.cloudfoundry.rivendell.cf.LogStreamer
import org.cloudfoundry.rivendell.logs.LogConsumer
import org.cloudfoundry.rivendell.statistics.LOGS_CONSUMED
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.metrics.CounterService
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration

const val VALID_MESSAGE_PATTERN = "Printer"

@Component
open class LogSink @Autowired constructor(
    val cfApplicationEnv: CfApplicationEnv,
    val logStreamer: LogStreamer,
    val counterService: CounterService
) : LogConsumer {
    @Value("\${rivendell.consumption.post-production-wait-time-millis}")
    private var postProductionWaitTime: Long = 10_000L

    override fun consume(productionCompleteNotifier: Mono<Unit>): Mono<Long> {
        return logStreamer.logStreamForApplication(cfApplicationEnv.name)
            .filter { it.message.contains(VALID_MESSAGE_PATTERN) }
            .doOnNext { counterService.increment(LOGS_CONSUMED) }
            .takeUntilOther(
                Mono
                    .delay(Duration.ofMillis(postProductionWaitTime))
                    .delaySubscription(productionCompleteNotifier)
            )
            .count()
    }
}
