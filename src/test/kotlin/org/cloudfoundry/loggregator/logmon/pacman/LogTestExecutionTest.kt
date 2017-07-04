package org.cloudfoundry.loggregator.logmon.pacman

import org.cloudfoundry.loggregator.logmon.statistics.LOGS_CONSUMED
import org.cloudfoundry.loggregator.logmon.statistics.LOGS_PRODUCED
import org.cloudfoundry.loggregator.logmon.support.any
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.boot.actuate.metrics.CounterService
import reactor.core.publisher.Mono

@RunWith(MockitoJUnitRunner::class)
class LogTestExecutionTest {
    @Mock
    private lateinit var printer: Printer

    @Mock
    private lateinit var logSink: LogSink

    @Mock
    private lateinit var counterService: CounterService

    @InjectMocks
    private lateinit var logTest: LogTestExecution

    @Test
    fun runTest_shouldClearAllLogmonMetrics() {
        `when`(logSink.consume(any())).thenReturn(Mono.just(5))
        logTest.runTest()
        verify(counterService).reset(LOGS_PRODUCED)
        verify(counterService).reset(LOGS_CONSUMED)
    }
}
