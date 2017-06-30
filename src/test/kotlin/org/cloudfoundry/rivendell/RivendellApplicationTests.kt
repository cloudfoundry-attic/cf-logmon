package org.cloudfoundry.rivendell

import org.assertj.core.api.Assertions.assertThat
import org.cloudfoundry.rivendell.cf.ApplicationFinder
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.io.ByteArrayOutputStream
import java.io.PrintStream

@RunWith(SpringRunner::class)
@SpringBootTest
class RivendellApplicationTests {

    @MockBean
    private lateinit var applicationFinder: ApplicationFinder

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
            printer.print()
            printer.print()
            printer.print()
            val lines = outContent.toString().trim().split("\n")

            assertThat(lines.size).isEqualTo(3)
            lines.forEach { assertThat(it).contains("They are taking the Hobbits to Eisengard!") }
        } catch (e: InterruptedException) {
            e.printStackTrace()
            fail()
        }
    }
}
