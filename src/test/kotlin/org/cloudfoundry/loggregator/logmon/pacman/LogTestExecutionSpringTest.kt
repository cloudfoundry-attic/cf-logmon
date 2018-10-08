package org.cloudfoundry.loggregator.logmon.pacman;

import org.cloudfoundry.loggregator.logmon.statistics.LogTestExecutionsRepo;
import org.cloudfoundry.loggregator.logmon.support.any
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith;
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.Metric
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.TaskScheduler
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest
abstract class LogTestExecutionSpringTest {
    @MockBean
    protected lateinit var printer: Printer

    @MockBean
    protected lateinit var logSink: LogSink

    @MockBean
    protected lateinit var counterService: CounterService

    @MockBean
    protected lateinit var metricRepository: MetricRepository

    @MockBean
    protected lateinit var logTestExecutionsRepo: LogTestExecutionsRepo

    @MockBean
    private lateinit var taskScheduler: TaskScheduler

    @Autowired
    protected lateinit var logTest: LogTestExecution

    protected open var numPellets: Int = -1

    @Before
    fun setUp() {
        `when`(logSink.consume(any<Mono<Unit>>())).thenReturn(
            Mono.delay(Duration.ofMillis(1500)).then(Mono.just(numPellets.toLong()))
        )
        `when`(metricRepository.findOne(anyString())).thenReturn(Metric("", 5, Date()))
    }
}

@SpringBootTest(properties = arrayOf(
    "logmon.production.log-cycles=2",
    "logmon.production.log-duration-millis=1000",
    "logmon.production.initial-delay-millis=0"
))
@ActiveProfiles("test")
class QuietAppTest : LogTestExecutionSpringTest() {
    override var numPellets: Int = 2

    @Test
    fun runTest_shouldUseTheCorrectProfile() {
        logTest.runTest()

        verify(printer, times(2)).produce()
    }
}

@SpringBootTest(properties = arrayOf(
    "logmon.production.log-cycles=1000",
    "logmon.production.log-duration-millis=1000",
    "logmon.production.initial-delay-millis=0"
))
class NormalAppTest : LogTestExecutionSpringTest() {
    override var numPellets: Int = 1000

    @Test
    fun runTest_shouldUseTheCorrectProfile() {
        logTest.runTest()

        verify(printer, times(1000)).produce()
    }
}

@SpringBootTest(properties = arrayOf(
    "logmon.production.log-cycles=5000",
    "logmon.production.log-duration-millis=1000",
    "logmon.production.initial-delay-millis=0"
))
class NoisyAppTest : LogTestExecutionSpringTest() {
    override var numPellets: Int = 5000

    @Test
    fun runTest_shouldUseTheCorrectProfile() {
        logTest.runTest()

        verify(printer, times(5000)).produce()
    }
}
