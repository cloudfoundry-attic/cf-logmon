package org.cloudfoundry.loggregator.logmon.pacman

import com.nhaarman.mockito_kotlin.argumentCaptor
import org.assertj.core.api.Assertions.assertThat
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
        verify(metricRepository).set(Metric(LAST_EXECUTION_TIME, 0, any(Date::class.java)))
    }

    @Test
    fun runTest_shouldAddTheExecutionResultsToTheRepo() {
        logTest.runTest()

        argumentCaptor<LogTestExecutionResults>().apply {
            verify(logTestExecutionsRepo).save(capture())

            assertThat(firstValue.logsProduced).isEqualTo(10_000)
            assertThat(firstValue.logsConsumed).isEqualTo(10_000)
            assertThat(firstValue.productionDuration).isEqualTo(10_000)
        }
    }
}
