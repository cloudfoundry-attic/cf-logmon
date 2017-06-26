package org.cloudfoundry.rivendell

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.io.ByteArrayOutputStream
import java.io.PrintStream

@RunWith(SpringRunner::class)
@SpringBootTest
class RivendellApplicationTests {

    private val outContent = ByteArrayOutputStream()

    @Before
    fun setUpStreams() {
        System.setOut(PrintStream(outContent))
    }

    @After
    fun cleanUpStreams() {
        System.setOut(null)
    }

    @Test
    fun contextLoads() {
    }

    @Test
    fun theApp_shouldPrintToStandardOutAllTheTimes() {
        try {
            Thread.sleep(5000)

            val lines = outContent.toString().trim().split("\n")

            lines.forEach { assertThat(it).contains("They are taking the Hobbits to Eisengard!") }
        } catch (e: InterruptedException) {
            e.printStackTrace()
            fail()
        }
    }
}
