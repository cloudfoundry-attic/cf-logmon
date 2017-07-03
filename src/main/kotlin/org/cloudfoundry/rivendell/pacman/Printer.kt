package org.cloudfoundry.rivendell.pacman

import org.cloudfoundry.rivendell.logs.LogProducer
import org.cloudfoundry.rivendell.statistics.LOGS_PRODUCED
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.metrics.CounterService
import org.springframework.stereotype.Component

@Component
open class Printer @Autowired constructor(private val counterService: CounterService) : LogProducer {
    private val log = LoggerFactory.getLogger(Printer::class.java)

    override fun produce() {
        counterService.increment(LOGS_PRODUCED)
        log.info("They are taking the Hobbits to Eisengard!")
    }
}
