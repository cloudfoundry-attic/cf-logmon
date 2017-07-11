package org.cloudfoundry.loggregator.logmon

import org.cloudfoundry.loggregator.logmon.statistics.LogTestExecutionResults
import org.cloudfoundry.loggregator.logmon.statistics.LogTestExecutionsRepo
import org.cloudfoundry.loggregator.logmon.statistics.StatisticsPresenter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class HomeController @Autowired constructor(
    private val logTestExecutionsRepo: LogTestExecutionsRepo,
    private val statistics: StatisticsPresenter
) {
    @GetMapping(path = arrayOf("/"), produces = arrayOf("text/html"))
    fun index(model: Model): String {
        val results = logTestExecutionsRepo.findAll()
        model.addAttribute("testResults", results)
        model.addAttribute("todaysReliability", statistics.reliability(results))
        return "index"
    }

    @GetMapping(path = arrayOf("/tests"), produces = arrayOf("application/json"))
    @ResponseBody
    fun testIndexJson(): List<LogTestExecutionResults> {
        return logTestExecutionsRepo.findAll()
    }
}
