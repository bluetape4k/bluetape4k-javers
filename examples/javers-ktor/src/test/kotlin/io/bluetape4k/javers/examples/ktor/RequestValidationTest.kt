package io.bluetape4k.javers.examples.ktor

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RequestValidationTest {

    @Test
    fun `positive decimal validation preserves arbitrary precision`() {
        "1E-400".requirePositiveBigDecimal("unitPrice") shouldBeEqualTo BigDecimal("1E-400")
    }

    @Test
    fun `positive decimal validation rejects zero and malformed values`() {
        assertFailsWith<IllegalArgumentException> {
            "0".requirePositiveBigDecimal("unitPrice")
        }
        assertFailsWith<IllegalArgumentException> {
            "not-a-number".requirePositiveBigDecimal("unitPrice")
        }
    }
}
