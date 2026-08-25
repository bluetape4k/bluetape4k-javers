package io.bluetape4k.javers.persistence.redis.repository

import io.bluetape4k.codec.encodeHexString
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Redis 로그에 audit 식별자를 원문으로 남기지 않기 위한 진단 포맷터입니다.
 */
internal object RedisIdentifierDiagnostics {
    private const val FINGERPRINT_LENGTH = 16
    private const val DIGEST_ALGORITHM = "SHA-256"

    fun format(value: String, type: String): String {
        val fingerprint = MessageDigest
            .getInstance(DIGEST_ALGORITHM)
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .encodeHexString()
            .take(FINGERPRINT_LENGTH)

        return "type=$type, fingerprint=$fingerprint, length=${value.length}"
    }
}
