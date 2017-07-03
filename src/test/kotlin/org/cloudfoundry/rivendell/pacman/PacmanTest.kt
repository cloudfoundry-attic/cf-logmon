package org.cloudfoundry.rivendell.pacman

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.cloudfoundry.rivendell.logs.LogConsumer
import org.cloudfoundry.rivendell.logs.LogProducer
import org.cloudfoundry.rivendell.support.any
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.runners.MockitoJUnitRunner
import reactor.core.publisher.Mono

@RunWith(MockitoJUnitRunner::class)
class PacmanTest {
    @Mock
    private lateinit var logConsumer: LogConsumer

    @Mock
    private lateinit var logProducer: LogProducer

    private val pacman: Pacman by lazy { Pacman(logProducer, logConsumer, 20) }

    @Before
    fun setUp() {
        `when`(logConsumer.consume(any())).thenReturn(Mono.just(20))
    }

    @Test
    fun pacman_producesTheCorrectNumberOfPellets() {
        pacman.begin()
        verify(logProducer, times(20)).produce()
    }

    @Test
    fun pacman_consumesTheCorrectNumberOfPellets() {
        pacman.begin()
        verify(logConsumer).consume(any())
    }

    @Test
    fun pacman_reportsLogsConsumed() {
        try {
            pacman.begin()
            assertThat(pacman.pelletsConsumed).isEqualTo(20)
        } catch(e: PacmanBedTimeException) {
            fail("Pacman did not eat all his pellets before bedtime: got ${pacman.pelletsConsumed}, expected ${20}")
        } catch(e: Exception) {
            fail("Something else went wrong: $e")
        }
    }

    @Test(expected = PacmanBedTimeException::class)
    fun pacman_whenNotAllLogsAreConsumed_throwsBedTimeException() {
        `when`(logConsumer.consume(any())).thenReturn(Mono.just(4))
        pacman.begin()
    }
}
