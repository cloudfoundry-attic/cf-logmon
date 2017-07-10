package org.cloudfoundry.loggregator.logmon.pacman

import org.cloudfoundry.loggregator.logmon.cf.CfApplicationEnv
import org.cloudfoundry.loggregator.logmon.cf.LogStreamer
import org.cloudfoundry.loggregator.logmon.logs.LogConsumer
import org.cloudfoundry.loggregator.logmon.statistics.LOGS_CONSUMED
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
    companion object {
        val log: Logger = LoggerFactory.getLogger(LogSink::class.java)
    }

    @Value("\${logmon.consumption.post-production-wait-time-millis}")
    private var postProductionWaitTime: Long = 10_000L

    override fun consume(productionCompleteNotifier: Mono<Unit>): Mono<Long> {
        log.info("Beginning consumption")
        val application = logStreamer.fetchApplicationByName(cfApplicationEnv.name)!!
        return logStreamer.logStreamForApplication(application)
            .filter { it.message.contains(VALID_MESSAGE_PATTERN) }
            .takeUntilOther(
                Mono.delay(Duration.ofMillis(postProductionWaitTime))
                    .delaySubscription(productionCompleteNotifier)
            )
            .doOnComplete { log.info("Consumption complete") }
            .count()
            .onErrorResume { Mono.just(-1) }
    }
}
