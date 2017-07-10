package org.cloudfoundry.loggregator.logmon.pacman

import org.assertj.core.api.Assertions.assertThat
import org.cloudfoundry.doppler.LogMessage
import org.cloudfoundry.doppler.MessageType
import org.cloudfoundry.loggregator.logmon.cf.CfApplicationEnv
import org.cloudfoundry.loggregator.logmon.cf.LogStreamer
import org.cloudfoundry.loggregator.logmon.cf.LogStreamer.Application
import org.cloudfoundry.loggregator.logmon.statistics.LOGS_CONSUMED
import org.cloudfoundry.loggregator.logmon.support.any
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.boot.actuate.metrics.CounterService
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import reactor.test.publisher.TestPublisher
import java.net.URI
import java.time.Duration
import java.time.Instant


@RunWith(MockitoJUnitRunner::class)
class LogSinkTest {
    @Mock
    private lateinit var logStreamer: LogStreamer

    @Mock
    private lateinit var counterService: CounterService

    private val appEnv = CfApplicationEnv().also {
        it.spaceId = "Middle Earth"
        it.name = "logmon"
        it.cfApi = URI("https://example.com")
    }

    @Before
    fun setUp() {
        `when`(logStreamer.fetchApplicationByName(any())).thenReturn(Application("foo", "bar", "baz"))
    }

    @Test
    fun consume_returnsNumberOfValidLogsConsumed() {
        `when`(logStreamer.logStreamForApplication(any())).thenReturn(Flux.just(
            message("$VALID_MESSAGE_PATTERN 1"),
            message("$VALID_MESSAGE_PATTERN 2"),
            message("NOT VALID"),
            message("$VALID_MESSAGE_PATTERN 3")
        ))

        val sink = LogSink(appEnv, logStreamer, counterService)
        assertThat(sink.consume(Mono.empty()).block()).isEqualTo(3)
    }

    @Test
    fun consume_runsForTenSecondsAfterTheProductionCompletes() {
        val productionCompletePublisher = TestPublisher.create<Unit>()
        val logGenerator = TestPublisher.create<LogMessage>()
        StepVerifier.withVirtualTime {
            `when`(logStreamer.logStreamForApplication(any())).thenReturn(logGenerator.flux())
            LogSink(appEnv, logStreamer, counterService).consume(productionCompletePublisher.mono())
        }
            .thenAwait(Duration.ofMillis(10_000))
            .then { repeat(5) { logGenerator.next(message("Printer - $it")) } }
            .expectNoEvent(Duration.ofMillis(10_000))
            .then { productionCompletePublisher.emit() }
            .expectNoEvent(Duration.ofMillis(9_999))
            .thenAwait(Duration.ofMillis(1))
            .expectNextMatches { count -> count == 5L }
            .verifyComplete()
    }

    @Test
    fun consume_whenLogStreamForApplicationThrows_continuesWithNANLogs() {
        val productionCompletePublisher = TestPublisher.create<Unit>()
        StepVerifier.withVirtualTime {
            `when`(logStreamer.logStreamForApplication(any())).thenReturn(Flux.error(NullPointerException()))
            LogSink(appEnv, logStreamer, counterService).consume(productionCompletePublisher.mono())
        }
            .expectNextMatches { count -> count == -1L }
            .verifyComplete()
    }

    private fun message(text: String): LogMessage = LogMessage.builder()
        .message(text)
        .messageType(MessageType.OUT)
        .timestamp(Instant.now().toEpochMilli())
        .build()
}
