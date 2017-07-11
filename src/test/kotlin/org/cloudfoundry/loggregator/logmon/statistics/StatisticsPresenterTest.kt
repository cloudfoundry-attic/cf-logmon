package org.cloudfoundry.loggregator.logmon.statistics

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.Duration
import java.time.Instant

@RunWith(JUnit4::class)
class StatisticsPresenterTest {
    private val statistics = StatisticsPresenter()

    @Test
    fun reliability_returnsAverageReliability() {
        val results = listOf(
            LogTestExecutionResults(10, 9, Instant.now(), 0.0),
            LogTestExecutionResults(10, 8, Instant.now(), 0.0),
            LogTestExecutionResults(10, 7, Instant.now(), 0.0),
            LogTestExecutionResults(10, 6, Instant.now(), 0.0)
        )

        assertThat(statistics.reliability(results)).isEqualTo(75)
    }

    @Test
    fun reliability_handlesFailedConsumptionRecords() {
        val results = listOf(
            LogTestExecutionResults(10, -1, Instant.now(), 0.0),
            LogTestExecutionResults(10, 8, Instant.now(), 0.0)
        )

        assertThat(statistics.reliability(results)).isEqualTo(80)
    }

    @Test
    fun runTime_returnsZeroWithAnEmptyList() {
        val results = emptyList<LogTestExecutionResults>()
        assertThat(statistics.runTime(results)).isEqualTo(Duration.ZERO)
    }

    @Test
    fun runTime_returnsTheTimeBetweenTheFirstAndLastTest() {
        val results = listOf(
            LogTestExecutionResults(10, 9, Instant.parse("2014-02-02T00:00:00Z"), 0.0),
            LogTestExecutionResults(10, 9, Instant.parse("2014-02-04T00:00:00Z"), 0.0)
        )
        assertThat(statistics.runTime(results)).isEqualTo(Duration.ofDays(2))
    }
}
