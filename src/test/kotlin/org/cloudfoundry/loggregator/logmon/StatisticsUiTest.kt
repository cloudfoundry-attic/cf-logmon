package org.cloudfoundry.loggregator.logmon

import org.assertj.core.api.Assertions.assertThat
import org.cloudfoundry.loggregator.logmon.pacman.LogTestExecution
import org.cloudfoundry.loggregator.logmon.statistics.*
import org.cloudfoundry.loggregator.logmon.support.getHtml
import org.cloudfoundry.loggregator.logmon.support.xpath
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.metrics.CounterService
import org.springframework.boot.actuate.metrics.Metric
import org.springframework.boot.actuate.metrics.repository.MetricRepository
import org.springframework.boot.context.embedded.LocalServerPort
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import java.time.Instant
import java.util.*


@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatisticsUiTest {
    @MockBean
    private lateinit var logTestExecution: LogTestExecution

    @LocalServerPort
    private var port: Int = -1

    private val baseUrl
        get() = "http://localhost:$port/"

    @Autowired
    private lateinit var counterService: CounterService

    @Autowired
    private lateinit var metricRepository: MetricRepository

    @Autowired
    private lateinit var http: TestRestTemplate

    @Before
    fun setUp() {
        metricRepository.set(Metric(LAST_EXECUTION_TIME, 0, Date(Instant.now().toEpochMilli())))
        metricRepository.setImmediate(LOG_WRITE_TIME_MILLIS, 10)
    }

    @After
    fun tearDown() {
        metricRepository.reset("counter.$LOGS_PRODUCED")
        metricRepository.reset("counter.$LOGS_CONSUMED")
        metricRepository.reset("counter.$LOG_WRITE_TIME_MILLIS")
        metricRepository.reset(LAST_EXECUTION_TIME)
    }

    @Test
    fun theApp_shouldDisplayNumberOfWrites() {
        var writes = page.xpath("//section[@class='metric metric-total-writes']")
        assertThat(writes.length).isEqualTo(1)
        assertThat(writes.text).isEqualToIgnoringCase("Logs Written: 0")

        counterService.increment(LOGS_PRODUCED)
        counterService.increment(LOGS_PRODUCED)
        counterService.increment(LOGS_PRODUCED)
        counterService.increment(LOGS_PRODUCED)
        counterService.increment(LOGS_PRODUCED)

        writes = page.xpath("//section[@class='metric metric-total-writes']")
        assertThat(writes.text).isEqualToIgnoringCase("Logs Written: 5")
    }

    @Test
    fun theApp_shouldDisplayNumberOfReads() {
        var reads = page.xpath("//section[@class='metric metric-total-reads']")
        assertThat(reads.length).isEqualTo(1)
        assertThat(reads.text).isEqualToIgnoringCase("Logs Read: 0")

        counterService.increment(LOGS_CONSUMED)
        counterService.increment(LOGS_CONSUMED)
        counterService.increment(LOGS_CONSUMED)

        reads = page.xpath("//section[@class='metric metric-total-reads']")
        assertThat(reads.text).isEqualToIgnoringCase("Logs Read: 3")
    }

    @Test
    fun theApp_shouldDisplayTheLastTestExecutionStartTime() {
        val now = Instant.now()
        metricRepository.set(Metric(LAST_EXECUTION_TIME, 1, Date(now.toEpochMilli())))

        val lastExecutionTime = page.xpath("//section[@class='metric metric-last-execution-time']")
        assertThat(lastExecutionTime.length).isEqualTo(1)
        assertThat(lastExecutionTime.text).isEqualToIgnoringCase("Last Test Execution Started At: $now")
    }

    @Test
    fun theApp_shouldDisplayLogBatchSizeOverWriteTime() {
        metricRepository.set(Metric("counter.$LOGS_PRODUCED", 10000, Date()))
        metricRepository.set(Metric("counter.$LOG_WRITE_TIME_MILLIS", 2000, Date()))

        val writeRate = page.xpath("//section[@class='metric metric-write-rate']")
        assertThat(writeRate.length).isEqualTo(1)
        assertThat(writeRate.text).isEqualToIgnoringCase("Write Rate: 10000 logs / 2000 ms = 5 logs/ms")
    }

    @Test
    fun theApp_doesNotDisplayTheWriteRateRatioWhenTheWriteTimeIsZero() {
        metricRepository.set(Metric("counter.$LOGS_PRODUCED", 10000, Date()))
        metricRepository.set(Metric("counter.$LOG_WRITE_TIME_MILLIS", 0, Date()))

        val writeRate = page.xpath("//section[@class='metric metric-write-rate']")
        assertThat(writeRate.length).isEqualTo(1)
        assertThat(writeRate.text).isEqualToIgnoringCase("Write Rate: 10000 logs / < 1 ms")
    }

    private val page: Document
        get() = http.getForObject(baseUrl, String::class.java).getHtml()

    private val NodeList.text: String
        get() = item(0).textContent.trim().replace(Regex("\\s+"), " ")
}
