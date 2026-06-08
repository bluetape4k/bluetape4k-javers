package io.bluetape4k.javers.persistence.kafka.repository

import io.bluetape4k.codec.encodeHexString
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

internal object KafkaSnapshotKeyDiagnostics {
    private const val FINGERPRINT_LENGTH = 16
    private const val DIGEST_ALGORITHM = "SHA-256"

    fun format(key: String): String {
        val fingerprint = MessageDigest
            .getInstance(DIGEST_ALGORITHM)
            .digest(key.toByteArray(StandardCharsets.UTF_8))
            .encodeHexString()
            .take(FINGERPRINT_LENGTH)

        return "keyFingerprint=$fingerprint, keyLength=${key.length}"
    }
}
