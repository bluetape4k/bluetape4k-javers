package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject

/**
 * Codec that converts a [JsonObject] to and from `Map<String, Any?>`.
 *
 * ## Contract
 * - Uses [JaversGsonElementConverter] for both conversion directions.
 * - [decode] always returns a non-null [JsonObject].
 *
 * ```kotlin
 * val codec = MapJaversCodec()
 * val map = codec.encode(jsonObject)
 * val restored = codec.decode(map)
 * // restored.toString() == jsonObject.toString()
 * ```
 */
class MapJaversCodec: JaversCodec<Map<String, Any?>> {

    override fun encode(jsonElement: JsonObject): Map<String, Any?> {
        return JaversGsonElementConverter.fromJsonObject(jsonElement)
    }

    override fun decode(encodedData: Map<String, Any?>): JsonObject {
        return JaversGsonElementConverter.toJsonObject(encodedData)
    }
}
