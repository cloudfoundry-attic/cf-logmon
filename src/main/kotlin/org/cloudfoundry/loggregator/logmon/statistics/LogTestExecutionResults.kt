package org.cloudfoundry.loggregator.logmon.statistics

import java.time.Instant

data class LogTestExecutionResults(
    val logsProduced: Int,
    val logsConsumed: Int,
    val startTime: Instant,
    val productionDuration: Double
) {
    val writeRateDisplay: String
        get() {
            return if (productionDuration == 0.0)
                "$logsProduced logs / < 1 ms"
            else
                """
                $logsProduced logs / ${String.format("%.02f", productionDuration)} ms
                =
                ${String.format("%.02f", logsProduced / productionDuration)} logs/ms
                """
        }
}
