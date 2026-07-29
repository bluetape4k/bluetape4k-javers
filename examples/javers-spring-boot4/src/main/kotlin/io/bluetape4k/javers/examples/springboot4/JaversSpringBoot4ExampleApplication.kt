package io.bluetape4k.javers.examples.springboot4

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * 명시적 JaVers + Exposed 예제를 위한 Spring Boot 4 entrypoint입니다.
 */
@SpringBootApplication
class JaversSpringBoot4ExampleApplication

fun main(args: Array<String>) {
    runApplication<JaversSpringBoot4ExampleApplication>(*args)
}
