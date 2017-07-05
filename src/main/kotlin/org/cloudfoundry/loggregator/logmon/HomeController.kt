package org.cloudfoundry.loggregator.logmon

import org.cloudfoundry.loggregator.logmon.statistics.LAST_EXECUTION_TIME
import org.cloudfoundry.loggregator.logmon.statistics.LOGS_CONSUMED
import org.cloudfoundry.loggregator.logmon.statistics.LOGS_PRODUCED
import org.cloudfoundry.loggregator.logmon.statistics.findCounter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.metrics.repository.MetricRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import java.time.Instant

@Controller
class HomeController @Autowired constructor(val metricRepository: MetricRepository) {
    val writeCount: Int
        get() = metricRepository.findCounter(LOGS_PRODUCED)
    val readCount: Int
        get() = metricRepository.findCounter(LOGS_CONSUMED)
    val lastExecutionTime: Instant
        get() = metricRepository.findOne(LAST_EXECUTION_TIME).timestamp.toInstant()

    @GetMapping(produces = arrayOf("text/html"))
    fun index(model: Model): String {
        model.addAttribute("writeCount", writeCount)
        model.addAttribute("readCount", readCount)
        model.addAttribute("lastExecutionTime", lastExecutionTime)
        return "index"
    }
}
