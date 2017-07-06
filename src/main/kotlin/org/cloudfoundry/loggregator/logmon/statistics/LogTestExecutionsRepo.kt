package org.cloudfoundry.loggregator.logmon.statistics

import org.springframework.stereotype.Repository

@Repository
open class LogTestExecutionsRepo {
    private var allResults: MutableList<LogTestExecutionResults> = mutableListOf()

    open fun findAll(): List<LogTestExecutionResults> {
        return allResults
    }

    open fun save(results: LogTestExecutionResults) {
        synchronized(this) {
            allResults.add(results)
        }
    }
}
