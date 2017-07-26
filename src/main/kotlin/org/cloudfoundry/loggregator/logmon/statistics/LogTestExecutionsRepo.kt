package org.cloudfoundry.loggregator.logmon.statistics

import org.springframework.stereotype.Repository
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.beans.factory.annotation.Autowired;

@Repository
open class LogTestExecutionsRepo {
    private var allResults: MutableList<LogTestExecutionResults> = mutableListOf()

    @Autowired
    private lateinit var gaugeService:GaugeService

    open fun findAll(): List<LogTestExecutionResults> {
        return allResults
    }

    open fun save(results: LogTestExecutionResults) {
        synchronized(this) {
           gaugeService.submit("logmon.logs_produced", results.logsProduced.toDouble());
           gaugeService.submit("logmon.logs_consumed", results.logsConsumed.toDouble());
           allResults.add(results)
        }
    }

    open fun deleteFirst(n: Int = 1) {
        synchronized(this) {
            repeat(n) {
                allResults.removeAt(0)
            }
        }
    }
}
