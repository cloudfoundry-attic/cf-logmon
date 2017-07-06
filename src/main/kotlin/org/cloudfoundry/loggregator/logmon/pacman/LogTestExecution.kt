package org.cloudfoundry.loggregator.logmon.pacman

import org.cloudfoundry.loggregator.logmon.statistics.LAST_EXECUTION_TIME
import org.cloudfoundry.loggregator.logmon.statistics.LOGS_CONSUMED
import org.cloudfoundry.loggregator.logmon.statistics.LOGS_PRODUCED
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.metrics.CounterService
import org.springframework.boot.actuate.metrics.Metric
import org.springframework.boot.actuate.metrics.repository.MetricRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*

@Component
open class LogTestExecution @Autowired constructor(
    private val printer: Printer,
    private val logSink: LogSink,
    private val counterService: CounterService,
    private val metricRepository: MetricRepository
) {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    @Value("\${logmon.production.logs-per-test}")
    private var totalPelletCount: Int = 10000

    @Scheduled(fixedDelayString = "\${logmon.time-between-tests-millis}", initialDelay = 1000)
    open fun runTest() {
        try {
            log.info("LogTest commencing: ${Date()}")
            metricRepository.set(Metric(LAST_EXECUTION_TIME, 0, Date()))
            counterService.reset(LOGS_PRODUCED)
            counterService.reset(LOGS_CONSUMED)

            Pacman(printer, logSink, totalPelletCount).begin().block()
            log.info("LogTest complete: ${Date()}")
        } catch (e: PacmanBedTimeException) {
            log.info(e.message)
        }
    }
}
