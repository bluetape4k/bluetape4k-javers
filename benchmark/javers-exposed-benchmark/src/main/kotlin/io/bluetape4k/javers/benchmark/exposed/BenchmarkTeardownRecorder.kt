package io.bluetape4k.javers.benchmark.exposed

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.APPEND
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * benchmark resource cleanup 한 건을 표현합니다.
 */
internal data class CleanupAction(
    val resource: String,
    val action: () -> Unit,
)

/**
 * benchmark teardown 실패를 원래 JMH 결과와 분리해 기록합니다.
 *
 * 각 cleanup은 독립적으로 실행하므로 한 resource의 실패가 다른 resource의
 * close를 건너뛰지 않습니다. 실패는 JSONL receipt와 warning log에 남고,
 * CI의 receipt validator가 이를 fail-closed로 처리합니다.
 */
internal object BenchmarkTeardownRecorder: KLogging() {

    const val REPORT_DIRECTORY_PROPERTY: String = "bluetape4k.benchmark.report-directory"
    const val REPORT_FILE_NAME: String = "teardown-failures.jsonl"

    private val writeLock = ReentrantLock()

    fun cleanup(owner: String, vararg actions: CleanupAction) {
        val failures = actions.mapNotNull { action ->
            try {
                action.action()
                null
            } catch (e: Exception) {
                TeardownFailure(owner = owner, resource = action.resource, cause = e)
            }
        }
        if (failures.isEmpty()) return

        val reportDirectory = Path.of(
            System.getProperty(REPORT_DIRECTORY_PROPERTY, "build/reports/benchmarks"),
        )
        writeLock.withLock {
            Files.createDirectories(reportDirectory)
            val report = reportDirectory.resolve(REPORT_FILE_NAME)
            Files.newBufferedWriter(
                report,
                StandardCharsets.UTF_8,
                CREATE,
                WRITE,
                APPEND,
            ).use { writer ->
                failures.forEach { failure ->
                    writer.appendLine(failure.toJsonLine())
                    log.warn(failure.cause) {
                        "Benchmark teardown failed. owner=${failure.owner}, resource=${failure.resource}"
                    }
                }
            }
        }
    }

    private data class TeardownFailure(
        val owner: String,
        val resource: String,
        val cause: Exception,
    ) {
        fun toJsonLine(): String {
            return "{" +
                "\"timestamp\":\"${escape(Instant.now().toString())}\", " +
                "\"owner\":\"${escape(owner)}\", " +
                "\"resource\":\"${escape(resource)}\", " +
                "\"message\":\"${escape(cause.message ?: cause::class.java.name)}\", " +
                "\"exception\":\"${escape(cause::class.java.name)}\"" +
                "}"
        }

        private fun escape(value: String): String = buildString(value.length + 8) {
            value.forEach { character ->
                when (character) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    in '\u0000'..'\u001F' -> append("\\u")
                        .append(character.code.toString(16).padStart(4, '0'))
                    else -> append(character)
                }
            }
        }
    }
}
