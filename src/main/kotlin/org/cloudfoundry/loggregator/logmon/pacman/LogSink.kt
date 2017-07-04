package org.cloudfoundry.loggregator.logmon.pacman

import org.cloudfoundry.loggregator.logmon.cf.CfApplicationEnv
import org.cloudfoundry.loggregator.logmon.cf.LogStreamer
import org.cloudfoundry.loggregator.logmon.logs.LogConsumer
import org.cloudfoundry.loggregator.logmon.statistics.LOGS_CONSUMED
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.metrics.CounterService
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

const val VALID_MESSAGE_PATTERN = "Printer"

@Component
open class LogSink @Autowired constructor(
    val cfApplicationEnv: CfApplicationEnv,
    val logStreamer: LogStreamer,
    val counterService: CounterService
) : LogConsumer {
    @Value("\${logmon.consumption.post-production-wait-time-millis}")
    private var postProductionWaitTime: Long = 10_000L

    override fun consume(productionCompleteNotifier: Mono<Unit>): Mono<Long> {
        val application = logStreamer.fetchApplicationByName(cfApplicationEnv.name)!!
        return logStreamer.logStreamForApplication(application)
            .doOnNext { println("${Instant.now()}: Message received") }
            .filter { it.message.contains(VALID_MESSAGE_PATTERN) }
            .doOnNext { counterService.increment(LOGS_CONSUMED) }
            .takeUntilOther(
                Mono.delay(Duration.ofMillis(postProductionWaitTime))
                    .delaySubscription(productionCompleteNotifier)
            )
            .count()
    }
}
