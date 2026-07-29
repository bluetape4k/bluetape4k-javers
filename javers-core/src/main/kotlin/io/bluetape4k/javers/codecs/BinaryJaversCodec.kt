package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.logging.KLogging

/**
 * [BinarySerializer]를 사용해 [JsonObject]를 byte array로 직렬화하는 codec입니다.
 *
 * ## 계약
 * - 직렬화하기 전에 [MapJaversCodec]으로 [JsonObject]를 `Map`으로 변환합니다.
 * - 역직렬화에 실패하면 [decode]에서 `null`을 반환합니다.
 *
 * ```kotlin
 * val codec = BinaryJaversCodec(BinarySerializers.Kryo)
 * val bytes = codec.encode(jsonObject)
 * val decoded = codec.decode(bytes)
 * // decoded != null
 * ```
 *
 * @property serializer 바이너리 직렬화와 역직렬화에 사용하는 [BinarySerializer]입니다.
 */
class BinaryJaversCodec(
    private val serializer: BinarySerializer,
): JaversCodec<ByteArray> {

    companion object: KLogging()

    private val mapCodec: MapJaversCodec = MapJaversCodec()

    override fun encode(jsonElement: JsonObject): ByteArray {
        return serializer.serialize(mapCodec.encode(jsonElement))
    }

    override fun decode(encodedData: ByteArray): JsonObject? {
        return serializer.deserialize<Map<String, Any?>>(encodedData)?.let {
            mapCodec.decode(it)
        }
    }
}
