package org.cloudfoundry.rivendell

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
        @Scheduled(fixedRate = 1000)
        fun print() {
            println("They are taking the Hobbits to Eisengard!")
        }
    }
}
