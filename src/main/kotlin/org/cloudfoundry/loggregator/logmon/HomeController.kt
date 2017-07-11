package org.cloudfoundry.loggregator.logmon

import org.cloudfoundry.loggregator.logmon.statistics.LogTestExecutionResults
import org.cloudfoundry.loggregator.logmon.statistics.LogTestExecutionsRepo
import org.cloudfoundry.loggregator.logmon.statistics.StatisticsPresenter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Controller
class HomeController @Autowired constructor(
    private val logTestExecutionsRepo: LogTestExecutionsRepo,
    private val statistics: StatisticsPresenter
) {
    @GetMapping(path = arrayOf("/"), produces = arrayOf("text/html"))
    fun index(model: Model): String {
        val results = logTestExecutionsRepo.findAll()
        val presenter = HomePagePresenter(results, statistics)
        model.addAttribute("page", presenter)
        return "index"
    }

    @GetMapping(path = arrayOf("/stats"), produces = arrayOf("text/html"))
    fun statsIndex(model: Model): String {
        model.addAttribute("testResults", logTestExecutionsRepo.findAll())
        return "stats/index"
    }

    @GetMapping(path = arrayOf("/tests"), produces = arrayOf("application/json"))
    @ResponseBody
    fun testIndex(): List<LogTestExecutionResults> {
        return logTestExecutionsRepo.findAll()
    }

    private class HomePagePresenter(val results: List<LogTestExecutionResults>, statistics: StatisticsPresenter) {
        val todaysReliability = statistics.reliability(
            results.filter { it.startTime > LocalDateTime.now().minusDays(1L).toInstant(ZoneOffset.UTC) }
        )

        val allTimeReliability = statistics.reliability(results)
        val allTimeDuration = statistics.runTime(results)
        val hasMultidayData = !allTimeDuration.isZero
        val allTimeDateRange
            get() =
            if (!allTimeDuration.isZero) {
                listOf(pp(results.first().startTime), pp(results.last().startTime)).joinToString(" - ")
            } else {
                ""
            }

        val today = pp(Instant.now())

        private fun pp(time: Instant): String {
            return SimpleDateFormat("M/d/YY").format(Date(time.toEpochMilli()))
        }
    }
}
