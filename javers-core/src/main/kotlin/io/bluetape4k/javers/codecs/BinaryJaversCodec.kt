package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.logging.KLogging

/**
 * Codec that serializes a [JsonObject] into bytes through [BinarySerializer].
 *
 * ## Contract
 * - Converts the JsonObject to a Map through [MapJaversCodec] before serialization.
 * - Returns `null` from [decode] when deserialization fails.
 *
 * ```kotlin
 * val codec = BinaryJaversCodec(BinarySerializers.Kryo)
 * val bytes = codec.encode(jsonObject)
 * val decoded = codec.decode(bytes)
 * // decoded != null
 * ```
 *
 * @property serializer [BinarySerializer] used for binary serialization
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
