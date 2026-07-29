package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * [JsonObject]를 JSON string으로 encode/decode하는 기본 codec입니다.
 *
 * ## 계약
 * - [encode]는 [JsonObject.toString]에 위임합니다.
 * - [decode]는 parse에 실패하면 `null`을 반환합니다.
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
