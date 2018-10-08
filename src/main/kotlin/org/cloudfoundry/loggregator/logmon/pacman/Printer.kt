package org.cloudfoundry.loggregator.logmon.pacman

import org.cloudfoundry.loggregator.logmon.logs.LogProducer
import org.cloudfoundry.loggregator.logmon.statistics.LOGS_PRODUCED
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.metrics.CounterService
import org.springframework.stereotype.Component

@Component
open class Printer @Autowired constructor(private val counterService: CounterService) : LogProducer {
    private val log = LoggerFactory.getLogger(Printer::class.java)

    @Value("\${logmon.production.log-byte-size}")
    private var logByteSize: Long = 256L

    override fun produce() {
        counterService.increment(LOGS_PRODUCED)
        log.info((1..logByteSize).map({ "0" }).joinToString(""))
    }
}
