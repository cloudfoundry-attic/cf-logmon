package org.cloudfoundry.loggregator.logmon

import org.assertj.core.api.Assertions.assertThat
import org.cloudfoundry.loggregator.logmon.pacman.LogTestExecution
import org.cloudfoundry.loggregator.logmon.statistics.LogTestExecutionResults
import org.cloudfoundry.loggregator.logmon.statistics.LogTestExecutionsRepo
import org.cloudfoundry.loggregator.logmon.support.getHtml
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
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.net.URI
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
    fun theApp_whenNoTestsHaveCompleted_shouldNotifyUserOfNoTests() {
        `when`(logTestExecutionsRepo.findAll()).thenReturn(listOf())
        assertThat(page().text).contains("No tests have completed yet.")
    }

    @Test
    fun theApp_shouldDisplayNumberOfWrites() {
        val writes = page().xpath("//section[@class='metric metric-total-writes']")
        assertThat(writes.length).isEqualTo(1)
        assertThat(writes.text).isEqualToIgnoringCase("Logs Written: 10000")
    }

    @Test
    fun theApp_shouldDisplayNumberOfReads() {
        val reads = page().xpath("//section[@class='metric metric-total-reads']")
        assertThat(reads.length).isEqualTo(1)
        assertThat(reads.text).isEqualToIgnoringCase("Logs Read: 9500")
    }

    @Test
    fun theApp_shouldDisplayTheLastTestExecutionStartTime() {
        val lastExecutionTime = page().xpath("//section[@class='metric metric-last-execution-time']")
        assertThat(lastExecutionTime.length).isEqualTo(1)
        assertThat(lastExecutionTime.text).isEqualToIgnoringCase("Last Test Execution Started At: $now")
    }

    @Test
    fun theApp_shouldDisplayLogBatchSizeOverWriteTime() {
        val writeRate = page().xpath("//section[@class='metric metric-write-rate']")
        assertThat(writeRate.length).isEqualTo(1)
        assertThat(writeRate.text).isEqualToIgnoringCase("Write Rate: 10000 logs / 2000.00 ms = 5.00 logs/ms")
    }

    @Test
    fun theApp_doesNotDisplayTheWriteRateRatioWhenTheWriteTimeIsZero() {
        `when`(logTestExecutionsRepo.findAll()).thenReturn(listOf(LogTestExecutionResults(10_000, 9_500, Instant.now(), 0.0)))

        val writeRate = page().xpath("//section[@class='metric metric-write-rate']")
        assertThat(writeRate.length).isEqualTo(1)
        assertThat(writeRate.text).isEqualToIgnoringCase("Write Rate: 10000 logs / < 1 ms")
    }

    @Test
    fun theApp_hasALinkToTheListOfLogTestExecutions() {
        `when`(logTestExecutionsRepo.findAll()).thenReturn(listOf(LogTestExecutionResults(10_000, 9_500, Instant.now(), 2_000.0)))
        val link = page().xpath("//a[@href='/tests']")
        val listPage = page(link.href)

        val rows = listPage.xpath("//table/tbody/tr")
        assertThat(rows.length).isEqualTo(1)
            .withFailMessage("Expected page to have <%s> rows, had <%s>.", 1, rows.length)

        val cells = listPage.xpath("//table/tbody/tr/td")
        assertThat(cells.length).isEqualTo(4)
            .withFailMessage("Expected page to have <%s> cells, had <%s>.", 4, cells.length)

        val ISO8601 = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z")
        assertThat(cells.item(0).textContent).matches(ISO8601)
        assertThat(cells.item(1).textContent).isEqualTo("10000")
        assertThat(cells.item(2).textContent).isEqualTo("9500")
        assertThat(cells.item(3).text).isEqualTo("10000 logs / 2000.00 ms = 5.00 logs/ms")
    }

    private fun page(path: String = "/"): Document {
        val request = RequestEntity.get(URI(baseUrl + path))
            .accept(MediaType.TEXT_HTML)
            .build()
        return http.exchange(request, String::class.java).body.getHtml()
    }

    private val Document.text: String
        get() = xpath("//body").item(0).textContent

    private val NodeList.text: String
        get() = item(0).text

    private val Node.text: String
        get() = textContent.trim().replace(Regex("\\s+"), " ")

    private val NodeList.href: String
        get() = item(0).attributes.getNamedItem("href").textContent
}
