package org.cloudfoundry.loggregator.logmon.pacman

import org.cloudfoundry.loggregator.logmon.logs.LogProducer
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.boot.actuate.metrics.repository.MetricRepository
import reactor.test.StepVerifier
import java.time.Duration

@RunWith(MockitoJUnitRunner::class)
class LogProductionTaskTest {
    @Mock
    private lateinit var logProducer: LogProducer
    @Mock
    private lateinit var metricRepository: MetricRepository

    @Test
    fun production_createsALogPeriodically() {
        val productionTask = LogProductionTask(logProducer, metricRepository, 2)

        StepVerifier.withVirtualTime { productionTask.get() }
            .expectSubscription()

            .thenAwait(Duration.ofMillis(1))
            .consumeNextWith { verify(logProducer).produce() }

            .expectNoEvent(Duration.ofMillis(499))
            .thenAwait(Duration.ofMillis(1))

            .consumeNextWith { verify(logProducer, times(2)).produce() }
            .verifyComplete()
    }

    @Test
    fun production_canHandleSubMillisecondResolution() {
        val productionTask = LogProductionTask(logProducer, metricRepository, 2001)

        StepVerifier.withVirtualTime { productionTask.get() }
            .expectSubscription()
            .thenAwait(Duration.ofMillis(1))
            .consumeNextWith { verify(logProducer, times(3)).produce() }
            .thenAwait(Duration.ofMillis(1))
            .consumeNextWith { verify(logProducer, times(5)).produce() }
            .thenAwait(Duration.ofMillis(998))
            .thenConsumeWhile { true }
            .verifyComplete()
    }
}
