package org.cloudfoundry.loggregator.logmon.pacman

import org.cloudfoundry.loggregator.logmon.statistics.*
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
    private val metricRepository: MetricRepository,
    private val logTestExecutionsRepo: LogTestExecutionsRepo
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

            Pacman(printer, logSink, metricRepository, totalPelletCount).begin().block()
            logTestExecutionsRepo.save(LogTestExecutionResults(
                metricRepository.findCounter(LOGS_PRODUCED),
                metricRepository.findCounter(LOGS_CONSUMED),
                metricRepository.findOne(LAST_EXECUTION_TIME).timestamp.toInstant(),
                metricRepository.findCounter("counter.$LOG_WRITE_TIME_MILLIS")
            ))
            log.info("LogTest complete: ${Date()}")
        } catch (e: PacmanBedTimeException) {
            log.info(e.message)
        }
    }
}
