package org.cloudfoundry.loggregator.logmon.pacman

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.cloudfoundry.loggregator.logmon.logs.LogConsumer
import org.cloudfoundry.loggregator.logmon.logs.LogProducer
import org.cloudfoundry.loggregator.logmon.statistics.LOG_WRITE_TIME_MILLIS
import org.cloudfoundry.loggregator.logmon.support.any
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.boot.actuate.metrics.Metric
import org.springframework.boot.actuate.metrics.repository.MetricRepository
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration

@RunWith(MockitoJUnitRunner::class)
class PacmanTest {
    @Mock
    private lateinit var logConsumer: LogConsumer

    @Mock
    private lateinit var logProducer: LogProducer

    @Mock
    private lateinit var metricRepository: MetricRepository

    private val pacman: Pacman by lazy { Pacman(logProducer, logConsumer, metricRepository, 20, 2500) }

    @Before
    fun setUp() {
        `when`(logConsumer.consume(any())).thenReturn(Mono.just(20))
    }

    @Test
    fun pacman_beginsConsumptionAndThenStartsProduction() {
        StepVerifier.withVirtualTime { pacman.begin() }
            .thenAwait(Duration.ofMillis(2499))
            .then { verifyZeroInteractions(logProducer) }
            .then { verify(logConsumer).consume(any()) }
            .thenAwait(Duration.ofMillis(1))
            .thenAwait(Duration.ofMillis(1000))
            .then { verify(logProducer, times(20)).produce() }
            .expectNext(20)
            .verifyComplete()
    }

    @Test
    fun pacman_reportsLogsConsumed() {
        var pelletsConsumed = 0L
        try {
            StepVerifier.withVirtualTime { pacman.begin() }
                .thenAwait(Duration.ofSeconds(2))
                .consumeNextWith { pelletsConsumed = it }
                .verifyComplete()
        } catch(e: PacmanBedTimeException) {
            fail("Pacman did not eat all his pellets before bedtime: got $pelletsConsumed, expected ${20}")
        } catch(e: Exception) {
            fail("Something else went wrong: $e")
        }
    }

    @Test
    fun pacman_whenNotAllLogsAreConsumed_throwsBedTimeException() {
        `when`(logConsumer.consume(any())).thenReturn(Mono.just(4))
        StepVerifier.withVirtualTime { pacman.begin() }
            .thenAwait(Duration.ofSeconds(2))
            .expectError(PacmanBedTimeException::class.java)
            .verify()
    }

    @Test
    fun pacman_shouldCaptureTheLogProductionFinishTime() {
        val captor = ArgumentCaptor.forClass(Metric::class.java)
        StepVerifier.withVirtualTime { pacman.begin() }
            .then { verifyZeroInteractions(metricRepository) }
            .thenAwait(Duration.ofMillis(2500))
            .thenAwait(Duration.ofMillis(1000))
            .then { verify(metricRepository).set(captor.capture()) }
            .consumeNextWith { }
            .verifyComplete()

        assertThat(captor.value.name).isEqualTo("counter.$LOG_WRITE_TIME_MILLIS")
        assertThat(captor.value.value.toDouble()).isEqualTo(1000.0)
    }
}
