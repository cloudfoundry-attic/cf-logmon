package org.cloudfoundry.rivendell

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class RivendellApplication {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            SpringApplication.run(RivendellApplication::class.java, *args)
        }
    }
}