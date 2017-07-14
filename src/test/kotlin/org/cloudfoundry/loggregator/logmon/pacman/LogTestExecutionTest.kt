package org.cloudfoundry.loggregator.logmon.pacman

import com.nhaarman.mockito_kotlin.argumentCaptor
import org.assertj.core.api.Assertions.assertThat
import org.cloudfoundry.loggregator.logmon.anomalies.AnomalyStateMachine
import org.cloudfoundry.loggregator.logmon.statistics.*
import org.cloudfoundry.loggregator.logmon.support.any
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.boot.actuate.metrics.CounterService
import org.springframework.boot.actuate.metrics.Metric
import org.springframework.boot.actuate.metrics.repository.MetricRepository
import reactor.core.publisher.Mono
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class LogTestExecutionTest {
    @Mock
    private lateinit var printer: Printer

    @Mock
    private lateinit var logSink: LogSink

    @Mock
    private lateinit var counterService: CounterService

    @Mock
    private lateinit var metricRepository: MetricRepository

    @Mock
    private lateinit var logTestExecutionsRepo: LogTestExecutionsRepo

    @Mock
    private lateinit var stateMachine: AnomalyStateMachine

    @InjectMocks
    private lateinit var logTest: LogTestExecution

    @Before
    fun setUp() {
        `when`(logSink.consume(any<Mono<Unit>>())).thenReturn(Mono.just(10_000))
        `when`(metricRepository.findOne(anyString())).thenReturn(Metric("metric", 10_000, Date()))
    }

    @Test
    fun runTest_shouldClearAllLogmonMetrics() {
        logTest.runTest()
        verify(counterService).reset(LOGS_PRODUCED)
        verify(counterService).reset(LOGS_CONSUMED)
    }

    @Test
    fun runTest_shouldSetTheLastExecutionTime() {
        logTest.runTest()

        argumentCaptor<Metric<Number>>().apply {
            verify(metricRepository, atLeastOnce()).set(capture())

            assertThat(firstValue.name).isEqualTo(LAST_EXECUTION_TIME)
            assertThat(firstValue.value).isEqualTo(0)
            assertThat(firstValue.timestamp).isInstanceOf(Date::class.java)
        }
    }

    @Test
    fun runTest_shouldAddTheExecutionResultsToTheRepoAndRecalculatesTheState() {
        logTest.runTest()

        tryWithTimeout(5000) {
            try {
                verify(stateMachine).recalculate()
                true
            } catch(e: Exception) {
                false
            }
        }

        argumentCaptor<LogTestExecutionResults>().apply {
            verify(logTestExecutionsRepo).save(capture())

            assertThat(firstValue.logsProduced).isEqualTo(10_000)
            assertThat(firstValue.logsConsumed).isEqualTo(10_000)
            assertThat(firstValue.productionDuration).isEqualTo(10_000.0)
        }
    }

    @Test
    fun runTest_whenTheTestFinishes_shouldSetLOGS_CONSUMED() {
        `when`(logSink.consume(any<Mono<Unit>>())).thenReturn(Mono.just(999))
        logTest.runTest()

        argumentCaptor<Metric<Long>>().apply {
            verify(metricRepository, atLeastOnce()).set(capture())

            val consumedUpdate = allValues.find { it.name == "counter.$LOGS_CONSUMED" }!!

            assertThat(consumedUpdate.value).isEqualTo(999L)
        }
    }

    private fun tryWithTimeout(timeToWait: Int, waitDoneCheck: () -> Boolean) {
        var tries = 0
        while (!waitDoneCheck.invoke()) {
            if (tries > timeToWait / 100) throw Exception("Thing never happened")
            tries++
            Thread.sleep(100)
        }
    }
}
