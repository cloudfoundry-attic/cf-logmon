package org.cloudfoundry.loggregator.logmon

import org.assertj.core.api.Assertions.assertThat
import org.cloudfoundry.loggregator.logmon.anomalies.AnomalyLevel
import org.cloudfoundry.loggregator.logmon.anomalies.AnomalyRepo
import org.cloudfoundry.loggregator.logmon.anomalies.ApplicationAnomaly
import org.cloudfoundry.loggregator.logmon.pacman.LogTestExecution
import org.cloudfoundry.loggregator.logmon.statistics.LogTestExecutionResults
import org.cloudfoundry.loggregator.logmon.statistics.LogTestExecutionsRepo
import org.cloudfoundry.loggregator.logmon.support.page
import org.cloudfoundry.loggregator.logmon.support.text
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.embedded.LocalServerPort
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.time.Instant

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardUiTest {
    @MockBean
    private lateinit var logTestExecution: LogTestExecution

    @MockBean
    private lateinit var logTestExecutionsRepo: LogTestExecutionsRepo

    @MockBean
    private lateinit var anomalyRepo: AnomalyRepo

    @LocalServerPort
    private var port: Int = -1

    private val baseUrl
        get() = "http://localhost:$port"

    @Autowired
    private lateinit var http: TestRestTemplate

    val now by lazy { Instant.now() }

    @Before
    fun setUp() {
        `when`(logTestExecutionsRepo.findAll()).thenReturn(listOf(
            LogTestExecutionResults(10_000, 9_500, now, 2000.0),
            LogTestExecutionResults(10_000, 8_777, now, 2000.0),
            LogTestExecutionResults(10_000, 5_000, now.minusSeconds(3600 * 24 * 2), 2000.0)
        ))

        `when`(anomalyRepo.findAll()).thenReturn(listOf(
            ApplicationAnomaly("Deploy Successful, collecting data", Instant.now(), AnomalyLevel.GREEN)
        ))
    }

    @Test
    fun theDashboard_displaysTodaysReliabilityRate() {
        val pageContent = page(http, baseUrl).text
        assertThat(pageContent).contains("91.39 %")
        assertThat(pageContent).contains("Today")
    }

    @Test
    fun theDashboard_displaysAllTimeReliabilityRate() {
        val pageContent = page(http, baseUrl).text
        assertThat(pageContent).contains("77.59 %")
        assertThat(pageContent).contains("Last 2 Days")
    }

    @Test
    fun theDashboard_displaysAnAnamolyForApplicationBoot() {
        val pageContent = page(http, baseUrl).text
        assertThat(pageContent).contains("Deploy Successful, collecting data")
    }
}
