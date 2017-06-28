package org.cloudfoundry.rivendell

import org.assertj.core.api.Assertions.assertThat
import org.cloudfoundry.rivendell.support.getHtml
import org.cloudfoundry.rivendell.support.xpath
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.embedded.LocalServerPort
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner


@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatisticsUiTest {
    @LocalServerPort
    private var port: Int = -1

    private val baseUrl
        get() = "http://localhost:$port/"

    @Autowired
    private lateinit var printer: Printer

    @Autowired
    private lateinit var http: TestRestTemplate

    @Test
    fun theApp_shouldDisplayNumberOfWrites() {
        var body = http.getForObject(baseUrl, String::class.java).getHtml()
        var titleNodes = body.xpath("//div[@class='metric metric-total-writes']")
        assertThat(titleNodes.length).isEqualTo(1)
        assertThat(titleNodes.item(0).textContent).isEqualToIgnoringCase("0")

        printer.print()
        printer.print()
        printer.print()
        printer.print()
        printer.print()

        body = http.getForObject(baseUrl, String::class.java).getHtml()
        titleNodes = body.xpath("//div[@class='metric metric-total-writes']")
        assertThat(titleNodes.item(0).textContent).isEqualToIgnoringCase("5")
    }
}
