package org.cloudfoundry.loggregator.logmon.statistics

import org.springframework.stereotype.Component
import java.time.Duration

@Component
class StatisticsPresenter {
    fun reliability(results: List<LogTestExecutionResults>): Double {
        val validResults = results.filter { it.logsConsumed >= 0 }
        if (validResults.count() == 0) {
            return 0.0
        } else {
            val rate = validResults
                .map { it.logsConsumed }
                .sum() / (validResults.first().logsProduced * validResults.count()).toDouble()
            return rate
        }
    }

    fun runTime(results: List<LogTestExecutionResults>): Duration {
        if (results.isEmpty()) return Duration.ZERO

        val sorted = results.sortedBy { it.startTime }
        return Duration.between(sorted.first().startTime, sorted.last().startTime)
    }
}
