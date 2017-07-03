package org.cloudfoundry.rivendell.pacman

import org.cloudfoundry.rivendell.statistics.LOGS_CONSUMED
import org.cloudfoundry.rivendell.statistics.LOGS_PRODUCED
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.metrics.CounterService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.Executors

@Component
open class LogTestExecution @Autowired constructor(
    private val printer: Printer,
    private val logSink: LogSink,
    internal val counterService: CounterService
) {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    @Value("\${rivendell.production.logs-per-test}")
    private var totalPelletCount: Int = 10000

    @Scheduled(fixedDelay = 60*1000, initialDelay = 1000)
    open fun runTest() {
        try {
            counterService.reset(LOGS_PRODUCED)
            counterService.reset(LOGS_CONSUMED)

            Pacman(printer, logSink, totalPelletCount, Executors.newFixedThreadPool(2)).begin()
        } catch (e: PacmanBedTimeException) {
            log.info(e.message)
        }
    }
}
