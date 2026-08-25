package io.bluetape4k.javers.benchmark.exposed

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readLines

class BenchmarkTeardownRecorderTest {

    @Test
    fun `cleanup continues after a resource failure and records a structured receipt`() {
        val reportDirectory = Files.createTempDirectory("benchmark-teardown")
        val previousReportDirectory = System.getProperty(BenchmarkTeardownRecorder.REPORT_DIRECTORY_PROPERTY)
        val events = mutableListOf<String>()
        try {
            System.setProperty(BenchmarkTeardownRecorder.REPORT_DIRECTORY_PROPERTY, reportDirectory.toString())

            BenchmarkTeardownRecorder.cleanup(
                owner = "fixture",
                CleanupAction("schema") {
                    events += "schema"
                    error("drop\u0000failed\u000B")
                },
                CleanupAction("datasource") {
                    events += "datasource"
                },
            )

            events shouldBeEqualTo listOf("schema", "datasource")
            val receipt = reportDirectory.resolve(BenchmarkTeardownRecorder.REPORT_FILE_NAME)
            receipt.toJsonLines() shouldHaveSize 1
            receipt.toJsonLines().single() shouldContain "\"owner\":\"fixture\""
            receipt.toJsonLines().single() shouldContain "\"resource\":\"schema\""
            receipt.toJsonLines().single() shouldContain "\"message\":\"drop\\u0000failed\\u000b\""
            receipt.toJsonLines().single().shouldNotContain("\u0000")
        } finally {
            if (previousReportDirectory == null) {
                System.clearProperty(BenchmarkTeardownRecorder.REPORT_DIRECTORY_PROPERTY)
            } else {
                System.setProperty(BenchmarkTeardownRecorder.REPORT_DIRECTORY_PROPERTY, previousReportDirectory)
            }
            reportDirectory.toFile().deleteRecursively()
        }
    }

    private fun Path.toJsonLines(): List<String> = readLines().filter { it.isNotBlank() }
}
