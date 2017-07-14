package org.cloudfoundry.loggregator.logmon.anomalies

import java.time.Instant

data class ApplicationAnomaly(val description: String, val timestamp: Instant, val level: AnomalyLevel)
