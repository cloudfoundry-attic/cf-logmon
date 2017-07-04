package org.cloudfoundry.loggregator.logmon

import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.metrics.CounterService
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository
import org.springframework.boot.actuate.metrics.repository.MetricRepository
import org.springframework.boot.actuate.metrics.writer.DefaultCounterService
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
open class LogmonApplication {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            SpringApplication.run(LogmonApplication::class.java, *args)
        }
    }

    @Configuration
    @EnableScheduling
    @Profile("!test")
    open class SchedulingConfiguration

    @Configuration
    @Profile("test")
    open class TestConfiguration

    @Bean
    open fun counterService(metricRepository: MetricRepository): CounterService = DefaultCounterService(metricRepository)

    @Bean
    open fun metricRepository(): MetricRepository = InMemoryMetricRepository()
}
