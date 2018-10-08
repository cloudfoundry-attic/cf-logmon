package org.cloudfoundry.loggregator.logmon

import org.assertj.core.api.Assertions.assertThat
import org.cloudfoundry.loggregator.logmon.pacman.LogTestExecution
import org.cloudfoundry.loggregator.logmon.pacman.Printer
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.context.TestPropertySource
import java.io.ByteArrayOutputStream
import java.io.PrintStream

@RunWith(SpringRunner::class)
@SpringBootTest
@TestPropertySource(properties = arrayOf(
    "logmon.production.log-byte-size=100"
))
class LogmonApplicationTests {

    @MockBean
    private lateinit var logTestExecution: LogTestExecution

    private val outContent = ByteArrayOutputStream()
    private var oldStdout: PrintStream? = null

    @Before
    fun setUpStreams() {
        oldStdout = System.out
        System.setOut(PrintStream(outContent))
    }

    @After
    fun cleanUpStreams() {
        System.setOut(oldStdout)
    }

    @Test
    fun contextLoads() {
    }

    @Autowired
    private lateinit var printer: Printer

    @Test
    fun theApp_shouldPrintToStandardOutAllTheTimes() {
        try {
            printer.produce()
            printer.produce()
            printer.produce()
            val lines = outContent.toString().trim().split("\n")

            assertThat(lines.size).isEqualTo(3)
            lines.forEach {
                // 100 bytes + log4j log metadata
                assertThat(it).hasSize(201)
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
            fail()
        }
    }
}
