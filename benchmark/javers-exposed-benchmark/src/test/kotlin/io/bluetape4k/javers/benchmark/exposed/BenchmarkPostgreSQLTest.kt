package io.bluetape4k.javers.benchmark.exposed

import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class BenchmarkPostgreSQLTest {

    @Test
    fun `container reuse is disabled by default`() {
        BenchmarkPostgreSQL.reuseEnabled(
            propertyValue = null,
            environment = emptyMap(),
        ).shouldBeFalse()
    }

    @Test
    fun `container reuse requires explicit local opt in`() {
        BenchmarkPostgreSQL.reuseEnabled(
            propertyValue = "true",
            environment = emptyMap(),
        ).shouldBeTrue()
    }

    @Test
    fun `CI cannot enable container reuse`() {
        BenchmarkPostgreSQL.reuseEnabled(
            propertyValue = "true",
            environment = mapOf("CI" to "true"),
        ).shouldBeFalse()

        BenchmarkPostgreSQL.reuseEnabled(
            propertyValue = "true",
            environment = mapOf("GITHUB_ACTIONS" to "true"),
        ).shouldBeFalse()
    }
}
