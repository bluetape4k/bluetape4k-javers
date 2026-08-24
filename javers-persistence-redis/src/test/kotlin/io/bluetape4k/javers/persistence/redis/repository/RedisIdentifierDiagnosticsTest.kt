package io.bluetape4k.javers.persistence.redis.repository

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test

class RedisIdentifierDiagnosticsTest {

    @Test
    fun `format keeps identifier type and length without exposing raw value`() {
        val rawIdentifier = "account:alice@example.com"

        val diagnostics = RedisIdentifierDiagnostics.format(rawIdentifier, "globalId")

        diagnostics shouldContain "type=globalId"
        diagnostics shouldContain "length=${rawIdentifier.length}"
        diagnostics shouldContain "fingerprint="
        diagnostics.shouldNotContain(rawIdentifier)
        diagnostics.shouldNotContain("alice")
        diagnostics.shouldNotContain("example.com")
    }

    @Test
    fun `format is stable for the same identifier`() {
        val rawIdentifier = "tenant-a/order-42"

        RedisIdentifierDiagnostics.format(rawIdentifier, "globalId") shouldBeEqualTo
            RedisIdentifierDiagnostics.format(rawIdentifier, "globalId")
    }
}
