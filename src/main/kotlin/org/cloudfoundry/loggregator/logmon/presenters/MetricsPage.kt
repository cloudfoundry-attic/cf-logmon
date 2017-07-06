package org.cloudfoundry.loggregator.logmon

import org.cloudfoundry.loggregator.logmon.statistics.*
import org.springframework.boot.actuate.metrics.repository.MetricRepository
import java.time.Instant

data class MetricsPage(val metricRepository: MetricRepository) {
    val writeCount: Int
        get() = metricRepository.findCounter(LOGS_PRODUCED)
    val readCount: Int
        get() = metricRepository.findCounter(LOGS_CONSUMED)
    val lastExecutionTime: Instant
        get() = metricRepository.findOne(LAST_EXECUTION_TIME).timestamp.toInstant()
    val writeRateDisplay: String
        get() {
            return if (logWriteTime == 0)
                "$writeCount logs / < 1 ms"
            else
                "$writeCount logs / $logWriteTime ms = ${writeCount / logWriteTime} logs/ms"
        }

    private val logWriteTime: Int
        get() = metricRepository.findCounter(LOG_WRITE_TIME_MILLIS)
}
