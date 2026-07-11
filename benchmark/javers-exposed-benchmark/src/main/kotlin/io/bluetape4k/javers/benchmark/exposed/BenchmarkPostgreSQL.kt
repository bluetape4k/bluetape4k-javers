package io.bluetape4k.javers.benchmark.exposed

import io.bluetape4k.testcontainers.database.PostgreSQLServer
import io.bluetape4k.utils.ShutdownQueue

internal object BenchmarkPostgreSQL {
    const val REUSE_PROPERTY: String = "bluetape4k.testcontainers.reuse"

    val server: PostgreSQLServer by lazy {
        val reuse = reuseEnabled()
        PostgreSQLServer(reuse = reuse).apply {
            start()
            if (shouldRegisterForShutdown(reuse)) {
                ShutdownQueue.register(this)
            }
        }
    }

    internal fun shouldRegisterForShutdown(reuse: Boolean): Boolean = !reuse

    internal fun reuseEnabled(
        propertyValue: String? = System.getProperty(REUSE_PROPERTY),
        environment: Map<String, String> = System.getenv(),
    ): Boolean {
        val isCi = environment.containsKey("CI") || environment["GITHUB_ACTIONS"] == "true"
        return !isCi && propertyValue?.toBooleanStrictOrNull() == true
    }
}
