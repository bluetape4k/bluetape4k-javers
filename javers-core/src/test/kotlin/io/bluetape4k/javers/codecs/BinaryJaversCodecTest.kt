package io.bluetape4k.javers.codecs

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.io.serializer.BinarySerializer
import org.junit.jupiter.api.Test
import kotlin.coroutines.cancellation.CancellationException

class BinaryJaversCodecTest {

    @Test
    fun `malformed serializer payload returns null`() {
        val serializer = throwingSerializer<IllegalStateException>()

        BinaryJaversCodec(serializer).decode(byteArrayOf(0x01)).shouldBeNull()
    }

    @Test
    fun `cancellation is never converted to malformed payload`() {
        val serializer = object : BinarySerializer {
            override fun serialize(graph: Any?): ByteArray = byteArrayOf()

            override fun <T: Any> deserialize(bytes: ByteArray?): T? {
                throw CancellationException("cancelled")
            }
        }

        assertFailsWith<CancellationException> {
            BinaryJaversCodec(serializer).decode(byteArrayOf(0x01))
        }
    }

    private inline fun <reified T: Exception> throwingSerializer(): BinarySerializer = object : BinarySerializer {
        override fun serialize(graph: Any?): ByteArray = byteArrayOf()

        override fun <R: Any> deserialize(bytes: ByteArray?): R? {
            throw T::class.java.getDeclaredConstructor(String::class.java).newInstance("malformed")
        }
    }
}
