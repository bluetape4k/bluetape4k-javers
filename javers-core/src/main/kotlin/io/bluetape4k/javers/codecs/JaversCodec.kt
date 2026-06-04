package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject

/**
 * Codec interface that encodes and decodes a JaVers [JsonObject] as [T].
 *
 * ## Contract
 * - [encode] converts a [JsonObject] into the target representation.
 * - [decode] restores encoded data into a [JsonObject], or returns `null` on failure.
 *
 * ```kotlin
 * val codec: JaversCodec<String> = JaversCodecs.String
 * val encoded = codec.encode(jsonObject)
 * val decoded = codec.decode(encoded)
 * // decoded.toString() == jsonObject.toString()
 * ```
 */
interface JaversCodec<T: Any> {

    /**
     * Encodes a [JsonObject] as [T].
     */
    fun encode(jsonElement: JsonObject): T

    /**
     * Decodes encoded data into a [JsonObject], or returns `null` on failure.
     */
    fun decode(encodedData: T): JsonObject?

}
