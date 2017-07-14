package org.cloudfoundry.loggregator.logmon.statistics

import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import org.cloudfoundry.loggregator.logmon.anomalies.AnomalyLevel
import org.cloudfoundry.loggregator.logmon.anomalies.AnomalyRepo
import org.cloudfoundry.loggregator.logmon.anomalies.AnomalyStateMachine
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.runners.MockitoJUnitRunner
import java.time.Instant

@RunWith(MockitoJUnitRunner::class)
class AnomalyStateMachineTest {
    @Mock
    private lateinit var logTestExecutionsRepo: LogTestExecutionsRepo

    @Mock
    private lateinit var anomalyRepo: AnomalyRepo

    private val statistics = StatisticsPresenter()

    private val anomalyStateMachine: AnomalyStateMachine by lazy {
        AnomalyStateMachine(anomalyRepo, logTestExecutionsRepo, statistics)
    }

    @Test
    fun recalculate_whenTheLastNTestsAreAboveTheGreenThreshold_createsAnAnomaly() {
        `when`(logTestExecutionsRepo.findAll()).thenReturn(listOf(
            LogTestExecutionResults(1000, 1000, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 1000, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 1000, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 1000, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 1000, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 1000, Instant.now(), 0.0)
        ))

        anomalyStateMachine.recalculate()

        verifyZeroInteractions(anomalyRepo)
    }

    @Test
    fun recalculate_whenTheLastNTestsFallBelowAThreshold_createsAnAnomaly() {
        `when`(logTestExecutionsRepo.findAll()).thenReturn(listOf(
            LogTestExecutionResults(1000, 1000, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 890, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 890, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 890, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 890, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 890, Instant.now(), 0.0)
        ))

        anomalyStateMachine.recalculate()

        verify(anomalyRepo).save(
            "The average reliability rate for the last 5 tests is 89%. " +
                "Click \"Review Data\" in the chart to see more info on the logs.",
            AnomalyLevel.RED
        )
    }

    @Test
    fun recalculate_whenThereAreFewerThanNTests_doesNothing() {
        `when`(logTestExecutionsRepo.findAll()).thenReturn(listOf(
            LogTestExecutionResults(1000, 890, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 890, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 890, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 890, Instant.now(), 0.0)
        ))

        anomalyStateMachine.recalculate()

        verifyZeroInteractions(anomalyRepo)
    }

    @Test
    fun recalculate_whenTheStateIsAlreadyRed_doesNotCreateANewAnomaly() {
        `when`(logTestExecutionsRepo.findAll()).thenReturn(listOf(
            LogTestExecutionResults(1000, 1000, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 890, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 890, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 890, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 890, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 890, Instant.now(), 0.0)
        ))

        anomalyStateMachine.recalculate()
        anomalyStateMachine.recalculate()

        verify(anomalyRepo, times(1)).save(
            "The average reliability rate for the last 5 tests is 89%. " +
                "Click \"Review Data\" in the chart to see more info on the logs.",
            AnomalyLevel.RED
        )
    }

    @Test
    fun recalculate_whenTheLastNTestsFallWithinTheYellowThreshold_createsAYellowAnomaly() {
        `when`(logTestExecutionsRepo.findAll()).thenReturn(listOf(
            LogTestExecutionResults(1000, 1000, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 950, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 950, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 950, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 950, Instant.now(), 0.0),
            LogTestExecutionResults(1000, 950, Instant.now(), 0.0)
        ))

        anomalyStateMachine.recalculate()
        anomalyStateMachine.recalculate()

        verify(anomalyRepo, times(1)).save(
            "Reliability Rate 95%",
            AnomalyLevel.YELLOW
        )
    }
}
