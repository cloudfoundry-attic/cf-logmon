package org.cloudfoundry.loggregator.logmon.statistics

import org.springframework.stereotype.Component
import java.time.Duration

@Component
class StatisticsPresenter {
    fun reliability(results: List<LogTestExecutionResults>): String {
        val rate = results.filter { it.logsConsumed >= 0 }
            .map { it.logsConsumed / it.logsProduced.toDouble() }
            .average()
        return String.format("%.2f", 100 * rate)
    }

    fun runTime(results: List<LogTestExecutionResults>): Duration {
        if (results.isEmpty()) return Duration.ZERO

        val sorted = results.sortedBy { it.startTime }
        return Duration.between(sorted.first().startTime, sorted.last().startTime)
    }
}
