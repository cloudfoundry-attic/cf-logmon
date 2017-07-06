package org.cloudfoundry.loggregator.logmon.statistics

import java.time.Instant

data class LogTestExecutionResults(
    val logsProduced: Int,
    val logsConsumed: Int,
    val startTime: Instant,
    val productionDuration: Int
) {
    val writeRateDisplay: String
        get() {
            return if (productionDuration == 0)
                "$logsProduced logs / < 1 ms"
            else
                "$logsProduced logs / $productionDuration ms = ${logsProduced / productionDuration} logs/ms"
        }
}
