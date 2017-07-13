package org.cloudfoundry.loggregator.logmon.anomalies

import org.springframework.stereotype.Component
import java.time.Instant

@Component
open class AnomalyRepo {
    private var anomalies: MutableList<ApplicationAnomaly> = mutableListOf()

    open fun findAll(): List<ApplicationAnomaly> {
        return anomalies
    }

    open fun save(description: String) {
        anomalies.add(ApplicationAnomaly(description, Instant.now()))
    }
}
