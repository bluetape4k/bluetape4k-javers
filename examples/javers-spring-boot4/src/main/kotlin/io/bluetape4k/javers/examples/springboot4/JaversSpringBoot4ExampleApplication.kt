package io.bluetape4k.javers.examples.springboot4

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Spring Boot 4 entrypoint for the explicit JaVers + Exposed example.
 */
@SpringBootApplication
class JaversSpringBoot4ExampleApplication

fun main(args: Array<String>) {
    runApplication<JaversSpringBoot4ExampleApplication>(*args)
}
