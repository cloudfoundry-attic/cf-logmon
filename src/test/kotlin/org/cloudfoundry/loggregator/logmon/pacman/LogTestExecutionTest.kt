package org.cloudfoundry.loggregator.logmon.pacman

import org.cloudfoundry.loggregator.logmon.statistics.LAST_EXECUTION_TIME
import org.cloudfoundry.loggregator.logmon.statistics.LOGS_CONSUMED
import org.cloudfoundry.loggregator.logmon.statistics.LOGS_PRODUCED
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

    @InjectMocks
    private lateinit var logTest: LogTestExecution

    @Before
    fun setUp() {
        `when`(logSink.consume(any<Mono<Unit>>())).thenReturn(Mono.just(5))
    }

    @Test
    fun runTest_shouldClearAllLogmonMetrics() {
        `when`(logSink.consume(any<Mono<Unit>>())).thenReturn(Mono.just(5))
        logTest.runTest()
        verify(counterService).reset(LOGS_PRODUCED)
        verify(counterService).reset(LOGS_CONSUMED)
    }

    @Test
    fun runTest_shouldSetTheLastExecutionTime() {
        logTest.runTest()
        verify(metricRepository).set(Metric(LAST_EXECUTION_TIME, 0, any(Date::class.java)))
    }
}
