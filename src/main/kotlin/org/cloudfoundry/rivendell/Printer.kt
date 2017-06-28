package org.cloudfoundry.rivendell

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.metrics.CounterService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
open class Printer @Autowired constructor(private val counterService: CounterService) {
    private val log = LoggerFactory.getLogger(Printer::class.java)

    @Scheduled(fixedDelay = 1000, initialDelay = 1000)
    fun print() {
        counterService.increment("rivendell.logs.written")
        log.info("They are taking the Hobbits to Eisengard!")
    }
}
