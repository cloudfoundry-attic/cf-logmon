package org.cloudfoundry.loggregator.logmon.statistics

import org.springframework.boot.actuate.metrics.Metric
import org.springframework.boot.actuate.metrics.repository.MetricRepository
import java.util.*

fun MetricRepository.findCounter(metricName: String): Int {
    return findOne("counter.$metricName")?.value?.toInt() ?: 0
}

fun MetricRepository.setImmediate(metricName: String, value: Number) {
    this.set(Metric("counter.$metricName", value, Date()))
}
