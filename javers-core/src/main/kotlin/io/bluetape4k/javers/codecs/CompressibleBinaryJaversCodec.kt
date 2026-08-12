package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject
import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import kotlin.coroutines.cancellation.CancellationException


/**
 * [BinaryJaversCodec]의 출력값을 [Compressor]로 압축하는 codec입니다.
 *
 * ## 동작 / 계약
 * - encode: innerCodec으로 직렬화한 뒤 압축합니다.
 * - decode: 압축을 해제한 뒤 innerCodec으로 역직렬화하며, malformed payload는 `null`을 반환합니다.
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
 * @property innerCodec 실제 바이너리 직렬화와 역직렬화를 수행하는 내부 codec입니다.
 * @property compressor 압축과 압축 해제에 사용하는 [Compressor]입니다. 기본값은 GZip입니다.
 */
class CompressibleBinaryJaversCodec(
    private val innerCodec: BinaryJaversCodec,
    private val compressor: Compressor = Compressors.GZip,
): JaversCodec<ByteArray> {

    override fun encode(jsonElement: JsonObject): ByteArray {
        return compressor.compress(innerCodec.encode(jsonElement))
    }

    override fun decode(encodedData: ByteArray): JsonObject? {
        return try {
            innerCodec.decode(compressor.decompress(encodedData))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}
