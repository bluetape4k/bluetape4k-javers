package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject
import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors


/**
 * A codec that compresses the output of [BinaryJaversCodec] using a [Compressor].
 *
 * ## Behavior / Contract
 * - encode: serialize via innerCodec → compress
 * - decode: decompress → deserialize via innerCodec
 *
 * ```kotlin
 * val codec = CompressibleBinaryJaversCodec(
 *     BinaryJaversCodec(BinarySerializers.Kryo),
 *     Compressors.LZ4
 * )
 * val compressed = codec.encode(jsonObject)
 * val decoded = codec.decode(compressed)
 * ```
 *
 * @property innerCodec the inner codec that performs binary serialization
 * @property compressor the [Compressor] used for compression/decompression (default: GZip)
 */
class CompressibleBinaryJaversCodec(
    private val innerCodec: BinaryJaversCodec,
    private val compressor: Compressor = Compressors.GZip,
): JaversCodec<ByteArray> {

    override fun encode(jsonElement: JsonObject): ByteArray {
        return compressor.compress(innerCodec.encode(jsonElement))
    }

    override fun decode(encodedData: ByteArray): JsonObject? {
        return innerCodec.decode(compressor.decompress(encodedData))
    }
}
