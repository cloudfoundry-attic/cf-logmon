package org.cloudfoundry.loggregator.logmon.anomalies

enum class AnomalyLevel {
    RED {
        override fun message(percentageReliability: Double) =
            String.format(
                "The average reliability rate for the last 5 tests is %.0f%%. " +
                    "Click \"Review Data\" in the chart to see more info on the logs.",
                percentageReliability
            )
    },
    YELLOW {
        override fun message(percentageReliability: Double): String =
            String.format("Reliability Rate %.0f%%", percentageReliability)
    },
    GREEN;

    open fun message(percentageReliability: Double) = ""
}
