package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject

/**
 * [JsonObject]와 `Map<String, Any?>` 사이를 양방향 변환하는 codec입니다.
 *
 * ## 계약
 * - 두 변환 방향 모두 [JaversGsonElementConverter]를 사용합니다.
 * - [decode]는 항상 non-null [JsonObject]를 반환합니다.
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
