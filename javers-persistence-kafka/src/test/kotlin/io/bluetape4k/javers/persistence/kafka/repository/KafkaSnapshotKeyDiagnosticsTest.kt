package io.bluetape4k.javers.persistence.kafka.repository

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test

class KafkaSnapshotKeyDiagnosticsTest {

    @Test
    fun `format returns stable fingerprint and key length without raw key`() {
        val rawKey = "account:alice@example.com"

        val diagnostics = KafkaSnapshotKeyDiagnostics.format(rawKey)

        diagnostics shouldBeEqualTo "keyFingerprint=eedf575c88031d11, keyLength=25"
        diagnostics shouldContain "keyFingerprint="
        diagnostics shouldContain "keyLength=${rawKey.length}"
        diagnostics.shouldNotContain(rawKey)
        diagnostics.shouldNotContain("alice")
        diagnostics.shouldNotContain("example.com")
    }

    @Test
    fun `format returns same fingerprint for same key`() {
        val rawKey = "tenant-a/order-42"

        KafkaSnapshotKeyDiagnostics.format(rawKey) shouldBeEqualTo KafkaSnapshotKeyDiagnostics.format(rawKey)
    }
}
