package org.cloudfoundry.loggregator.logmon

import org.assertj.core.api.Assertions.assertThat
import org.cloudfoundry.loggregator.logmon.pacman.LogTestExecution
import org.cloudfoundry.loggregator.logmon.statistics.LogTestExecutionResults
import org.cloudfoundry.loggregator.logmon.statistics.LogTestExecutionsRepo
import org.cloudfoundry.loggregator.logmon.support.page
import org.cloudfoundry.loggregator.logmon.support.text
import org.cloudfoundry.loggregator.logmon.support.xpath
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.time.Instant
import java.util.regex.Pattern


@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatisticsUiTest {
    @MockBean
    private lateinit var logTestExecution: LogTestExecution

    @MockBean
    private lateinit var logTestExecutionsRepo: LogTestExecutionsRepo

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
            LogTestExecutionResults(10_000, 9_500, now, 2000.0)
        ))
    }

    @Test
    fun theDashboard_hasALinkToTheListOfLogTestExecutions() {
        `when`(logTestExecutionsRepo.findAll()).thenReturn(listOf(LogTestExecutionResults(10_000, 9_500, Instant.now(), 2_000.0)))
        val rows = page(http, baseUrl, "/stats").xpath("//table/tbody/tr")
        assertThat(rows.length).isEqualTo(1)
            .withFailMessage("Expected page to have <%s> rows, had <%s>.", 1, rows.length)

        val cells = page(http, baseUrl, "/stats").xpath("//table/tbody/tr/td")
        assertThat(cells.length).isEqualTo(4)
            .withFailMessage("Expected page to have <%s> cells, had <%s>.", 4, cells.length)

        val ISO8601 = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z")
        assertThat(cells.item(0).textContent).matches(ISO8601)
        assertThat(cells.item(1).textContent).isEqualTo("10000")
        assertThat(cells.item(2).textContent).isEqualTo("9500")
        assertThat(cells.item(3).text).isEqualTo("10000 logs / 2000.00 ms = 5.00 logs/ms")
    }
}
