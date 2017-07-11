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
            LogTestExecutionResults(10_000, 9_500, now, 2000.0),
            LogTestExecutionResults(10_000, 8_700, now, 2000.0),
            LogTestExecutionResults(10_000, 9_000, now.minusSeconds(3600*24 + 1), 2000.0)
        ))
    }

    @Test
    fun theDashboard_displaysTodaysLossRate() {
        val pageContent = page().xpath("//body").text
        assertThat(pageContent).contains("91 %")
        assertThat(pageContent).contains("Today")
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

