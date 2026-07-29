package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject

/**
 * JaVers [JsonObject]를 [T] 형식으로 encode/decode하는 codec interface입니다.
 *
 * ## 계약
 * - [encode]는 [JsonObject]를 대상 표현 형식으로 변환합니다.
 * - [decode]는 encode된 데이터를 [JsonObject]로 복원하며, 실패하면 `null`을 반환합니다.
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
     * [JsonObject]를 [T] 형식으로 encode합니다.
     */
    fun encode(jsonElement: JsonObject): T

    /**
     * encode된 데이터를 [JsonObject]로 decode하며, 실패하면 `null`을 반환합니다.
     */
    fun decode(encodedData: T): JsonObject?

}
