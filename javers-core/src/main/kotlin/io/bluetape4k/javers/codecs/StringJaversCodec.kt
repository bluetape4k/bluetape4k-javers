package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Default codec that encodes and decodes a [JsonObject] as a JSON string.
 *
 * ## Contract
 * - [encode] delegates to [JsonObject.toString].
 * - [decode] returns `null` when parsing fails.
 *
 * ```kotlin
 * val codec = StringJaversCodec()
 * val encoded = codec.encode(jsonObject)
 * val decoded = codec.decode(encoded)
 * // decoded.toString() == jsonObject.toString()
 * ```
 */
open class StringJaversCodec: JaversCodec<String> {

    override fun encode(jsonElement: JsonObject): String {
        return jsonElement.toString()
    }

    override fun decode(encodedData: String): JsonObject? {
        return runCatching { JsonParser.parseString(encodedData) as JsonObject }.getOrNull()
    }
}
