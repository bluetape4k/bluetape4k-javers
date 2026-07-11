package io.bluetape4k.javers.benchmark.exposed

import io.bluetape4k.testcontainers.database.PostgreSQLServer
import io.bluetape4k.utils.ShutdownQueue

internal object BenchmarkPostgreSQL {
    const val REUSE_PROPERTY: String = "bluetape4k.testcontainers.reuse"

    val server: PostgreSQLServer by lazy {
        PostgreSQLServer(reuse = reuseEnabled()).apply {
            start()
            ShutdownQueue.register(this)
        }
    }

    internal fun reuseEnabled(
        propertyValue: String? = System.getProperty(REUSE_PROPERTY),
        environment: Map<String, String> = System.getenv(),
    ): Boolean {
        val isCi = environment.containsKey("CI") || environment["GITHUB_ACTIONS"] == "true"
        return !isCi && propertyValue?.toBooleanStrictOrNull() == true
    }
}
