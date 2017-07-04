package org.cloudfoundry.loggregator.logmon.statistics

import org.springframework.boot.actuate.metrics.repository.MetricRepository

fun MetricRepository.findCounter(metricName: String): Int {
    return findOne("counter.$metricName")?.value?.toInt() ?: 0
}
