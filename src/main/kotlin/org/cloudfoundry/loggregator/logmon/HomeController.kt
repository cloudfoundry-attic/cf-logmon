package org.cloudfoundry.loggregator.logmon

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.metrics.repository.MetricRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController @Autowired constructor(val metricRepository: MetricRepository) {
    @GetMapping(produces = arrayOf("text/html"))
    fun index(model: Model): String {
        model.addAttribute("metricsPage", MetricsPage(metricRepository))
        return "index"
    }
}
