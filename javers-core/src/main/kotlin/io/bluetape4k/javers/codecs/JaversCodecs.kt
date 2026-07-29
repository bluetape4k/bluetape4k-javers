package io.bluetape4k.javers.codecs

import io.bluetape4k.annotations.BluetapeObsoleteApi
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.javers.codecs.JaversCodecs.String

private const val JDK_CODEC_DEPRECATION_MESSAGE =
    "JDK serialization is obsolete because Java deserialization can execute unsafe gadget chains. " +
        "Use JaversCodecs.Fory or JaversCodecs.Kryo for trusted binary payloads, or JaversCodecs.String for JSON."

/**
 * 미리 구성된 [JaversCodec] instance를 제공하는 factory object입니다.
 *
 * ## 동작 / 계약
 * - 모든 codec은 최초 접근 시점에 lazy initialization됩니다.
 * - String 계열: 선택적 압축을 지원하는 JSON string 기반 encoding입니다.
 * - Binary 계열: 선택적 압축을 지원하며 BinarySerializer(Kryo/Fory)로 바이너리 직렬화합니다.
 * - JDK serialization codec은 obsolete compatibility bridge로만 유지합니다.
 * - Map 계열: [JsonObject]와 `Map<String, Any?>` 사이를 양방향 변환합니다.
 *
 * ```kotlin
 * val codec = JaversCodecs.LZ4String
 * val encoded = codec.encode(jsonObject)
 * val decoded = codec.decode(encoded)
 * // decoded.toString() == jsonObject.toString()
 * ```
 */
object JaversCodecs {

    /** 기본 codec입니다([String]). */
    val Default by lazy { String }

    // String codec 계열

    /** 압축하지 않는 plain JSON string codec입니다. */
    val String by lazy { StringJaversCodec() }

    /** GZip으로 압축하는 string codec입니다. */
    val GZipString by lazy { CompressibleStringJaversCodec(String, Compressors.GZip) }

    /** Deflate로 압축하는 string codec입니다. */
    val DeflateString by lazy { CompressibleStringJaversCodec(String, Compressors.Deflate) }

    /** LZ4로 압축하는 string codec입니다. */
    val LZ4String by lazy { CompressibleStringJaversCodec(String, Compressors.LZ4) }

    /** Snappy로 압축하는 string codec입니다. */
    val SnappyString by lazy { CompressibleStringJaversCodec(String, Compressors.Snappy) }

    /** Zstd로 압축하는 string codec입니다. */
    val ZstdString by lazy { CompressibleStringJaversCodec(String, Compressors.Zstd) }

    // Binary codec - JDK 직렬화 계열

    /**
     * JDK serialization 기반 binary codec입니다.
     *
     * @deprecated JDK deserialization은 신뢰할 수 없는 byte에 대해 안전하지 않습니다.
     * 저장 계약에 따라 [Fory], [Kryo], 또는 [String]을 사용하세요.
     */
    @BluetapeObsoleteApi
    @Deprecated(
        message = JDK_CODEC_DEPRECATION_MESSAGE,
        replaceWith = ReplaceWith("JaversCodecs.Fory"),
        level = DeprecationLevel.ERROR,
    )
    val Jdk by lazy { jdkBinaryCodec() }

    @BluetapeObsoleteApi
    @Deprecated(
        message = JDK_CODEC_DEPRECATION_MESSAGE,
        replaceWith = ReplaceWith("JaversCodecs.DeflateFory"),
        level = DeprecationLevel.ERROR,
    )
    val DeflateJdk by lazy { CompressibleBinaryJaversCodec(jdkBinaryCodec(), Compressors.Deflate) }

    @BluetapeObsoleteApi
    @Deprecated(
        message = JDK_CODEC_DEPRECATION_MESSAGE,
        replaceWith = ReplaceWith("JaversCodecs.GZipFory"),
        level = DeprecationLevel.ERROR,
    )
    val GZipJdk by lazy { CompressibleBinaryJaversCodec(jdkBinaryCodec(), Compressors.GZip) }

    @BluetapeObsoleteApi
    @Deprecated(
        message = JDK_CODEC_DEPRECATION_MESSAGE,
        replaceWith = ReplaceWith("JaversCodecs.LZ4Fory"),
        level = DeprecationLevel.ERROR,
    )
    val LZ4Jdk by lazy { CompressibleBinaryJaversCodec(jdkBinaryCodec(), Compressors.LZ4) }

    @BluetapeObsoleteApi
    @Deprecated(
        message = JDK_CODEC_DEPRECATION_MESSAGE,
        replaceWith = ReplaceWith("JaversCodecs.SnappyFory"),
        level = DeprecationLevel.ERROR,
    )
    val SnappyJdk by lazy { CompressibleBinaryJaversCodec(jdkBinaryCodec(), Compressors.Snappy) }

    @BluetapeObsoleteApi
    @Deprecated(
        message = JDK_CODEC_DEPRECATION_MESSAGE,
        replaceWith = ReplaceWith("JaversCodecs.ZstdFory"),
        level = DeprecationLevel.ERROR,
    )
    val ZstdJdk by lazy { CompressibleBinaryJaversCodec(jdkBinaryCodec(), Compressors.Zstd) }

    // Binary codec - Kryo 직렬화 계열

    /** Kryo serialization 기반 binary codec입니다. */
    val Kryo by lazy { BinaryJaversCodec(BinarySerializers.Kryo) }

    val DeflateKryo by lazy { CompressibleBinaryJaversCodec(Kryo, Compressors.Deflate) }
    val GZipKryo by lazy { CompressibleBinaryJaversCodec(Kryo, Compressors.GZip) }
    val LZ4Kryo by lazy { CompressibleBinaryJaversCodec(Kryo, Compressors.LZ4) }
    val SnappyKryo by lazy { CompressibleBinaryJaversCodec(Kryo, Compressors.Snappy) }
    val ZstdKryo by lazy { CompressibleBinaryJaversCodec(Kryo, Compressors.Zstd) }

    // Binary codec - Fory 직렬화 계열

    /** Fory serialization 기반 binary codec입니다. */
    val Fory by lazy { BinaryJaversCodec(BinarySerializers.Fory) }

    val DeflateFory by lazy { CompressibleBinaryJaversCodec(Fory, Compressors.Deflate) }
    val GZipFory by lazy { CompressibleBinaryJaversCodec(Fory, Compressors.GZip) }
    val LZ4Fory by lazy { CompressibleBinaryJaversCodec(Fory, Compressors.LZ4) }
    val SnappyFory by lazy { CompressibleBinaryJaversCodec(Fory, Compressors.Snappy) }
    val ZstdFory by lazy { CompressibleBinaryJaversCodec(Fory, Compressors.Zstd) }

    // Map codec 계열

    /** [JsonObject]와 `Map<String, Any?>` 사이를 변환하는 codec입니다. */
    val Map by lazy { MapJaversCodec() }

    @OptIn(BluetapeObsoleteApi::class)
    @Suppress("DEPRECATION")
    private fun jdkBinaryCodec(): BinaryJaversCodec = BinaryJaversCodec(BinarySerializers.Jdk)
}
