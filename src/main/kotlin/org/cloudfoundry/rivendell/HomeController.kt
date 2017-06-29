package org.cloudfoundry.rivendell

import org.cloudfoundry.rivendell.statistics.findCounter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.metrics.repository.MetricRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController @Autowired constructor(val metricRepository: MetricRepository) {
    val writeCount: Int
        get() = metricRepository.findCounter("rivendell.logs.written")
    val readCount: Int
        get() = metricRepository.findCounter("rivendell.logs.read")

    @GetMapping(produces = arrayOf("text/html"))
    fun index(model: Model): String {
        model.addAttribute("writeCount", writeCount)
        model.addAttribute("readCount", readCount)
        return "index"
    }
}
