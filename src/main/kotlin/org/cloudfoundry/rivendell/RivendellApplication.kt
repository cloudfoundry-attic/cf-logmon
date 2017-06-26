package org.cloudfoundry.rivendell

import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@SpringBootApplication
@EnableScheduling
open class RivendellApplication {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            SpringApplication.run(RivendellApplication::class.java, *args)
        }
    }

    @Component
    open class Printer {
        private val log = LoggerFactory.getLogger(Printer::class.java)

        @Scheduled(fixedRate = 1000)
        fun print() {
            log.info("They are taking the Hobbits to Eisengard!")
        }
    }
}
