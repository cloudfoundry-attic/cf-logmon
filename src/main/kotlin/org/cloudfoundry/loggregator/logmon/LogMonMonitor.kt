package org.cloudfoundry.loggregator.logmon

import org.cloudfoundry.loggregator.logmon.statistics.LogTestExecutionsRepo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class LogMonMonitor @Autowired constructor(private val resultsRepo: LogTestExecutionsRepo) {
    companion object {
        private val log = LoggerFactory.getLogger(LogMonMonitor::class.java)
    }

    @Scheduled(fixedDelay = 1000)
    fun sweepTheHouse() {
        val runtime = Runtime.getRuntime()
        if (runtime.freeMemory().toFloat() / runtime.totalMemory().toFloat() < 0.1) {
            val numItems = (resultsRepo.findAll().size * 0.1).toInt()
            log.info("Deleting first $numItems items from the resultsRepo")
            resultsRepo.deleteFirst(numItems)
        }
    }
}
