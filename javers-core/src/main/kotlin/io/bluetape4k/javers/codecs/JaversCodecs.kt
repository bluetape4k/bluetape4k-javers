package io.bluetape4k.javers.codecs

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.javers.codecs.JaversCodecs.String


/**
 * Factory object providing pre-configured [JaversCodec] instances.
 *
 * ## Behavior / Contract
 * - All codecs are lazily initialized on first access.
 * - String family: JSON-string-based encoding with optional compression.
 * - Binary family: binary serialization via BinarySerializer (Jdk/Kryo/Fory) with optional compression.
 * - Map family: bidirectional [JsonObject] ↔ `Map<String, Any?>` conversion.
 *
 * ```kotlin
 * val codec = JaversCodecs.LZ4String
 * val encoded = codec.encode(jsonObject)
 * val decoded = codec.decode(encoded)
 * // decoded.toString() == jsonObject.toString()
 * ```
 */
object JaversCodecs {

    /** Default codec ([String]). */
    val Default by lazy { String }

    // String Codecs

    /** Plain JSON string codec (no compression). */
    val String by lazy { StringJaversCodec() }

    /** GZip-compressed string codec. */
    val GZipString by lazy { CompressibleStringJaversCodec(String, Compressors.GZip) }

    /** Deflate-compressed string codec. */
    val DeflateString by lazy { CompressibleStringJaversCodec(String, Compressors.Deflate) }

    /** LZ4-compressed string codec. */
    val LZ4String by lazy { CompressibleStringJaversCodec(String, Compressors.LZ4) }

    /** Snappy-compressed string codec. */
    val SnappyString by lazy { CompressibleStringJaversCodec(String, Compressors.Snappy) }

    /** Zstd-compressed string codec. */
    val ZstdString by lazy { CompressibleStringJaversCodec(String, Compressors.Zstd) }

    // Binary Codecs - JDK Serialization

    /** JDK-serialization binary codec. */
    val Jdk by lazy { BinaryJaversCodec(BinarySerializers.Jdk) }

    val DeflateJdk by lazy { CompressibleBinaryJaversCodec(Jdk, Compressors.Deflate) }
    val GZipJdk by lazy { CompressibleBinaryJaversCodec(Jdk, Compressors.GZip) }
    val LZ4Jdk by lazy { CompressibleBinaryJaversCodec(Jdk, Compressors.LZ4) }
    val SnappyJdk by lazy { CompressibleBinaryJaversCodec(Jdk, Compressors.Snappy) }
    val ZstdJdk by lazy { CompressibleBinaryJaversCodec(Jdk, Compressors.Zstd) }

    // Binary Codecs - Kryo Serialization

    /** Kryo-serialization binary codec. */
    val Kryo by lazy { BinaryJaversCodec(BinarySerializers.Kryo) }

    val DeflateKryo by lazy { CompressibleBinaryJaversCodec(Kryo, Compressors.Deflate) }
    val GZipKryo by lazy { CompressibleBinaryJaversCodec(Kryo, Compressors.GZip) }
    val LZ4Kryo by lazy { CompressibleBinaryJaversCodec(Kryo, Compressors.LZ4) }
    val SnappyKryo by lazy { CompressibleBinaryJaversCodec(Kryo, Compressors.Snappy) }
    val ZstdKryo by lazy { CompressibleBinaryJaversCodec(Kryo, Compressors.Zstd) }

    // Binary Codecs - Fory Serialization

    /** Fory-serialization binary codec. */
    val Fory by lazy { BinaryJaversCodec(BinarySerializers.Fory) }

    val DeflateFory by lazy { CompressibleBinaryJaversCodec(Fory, Compressors.Deflate) }
    val GZipFory by lazy { CompressibleBinaryJaversCodec(Fory, Compressors.GZip) }
    val LZ4Fory by lazy { CompressibleBinaryJaversCodec(Fory, Compressors.LZ4) }
    val SnappyFory by lazy { CompressibleBinaryJaversCodec(Fory, Compressors.Snappy) }
    val ZstdFory by lazy { CompressibleBinaryJaversCodec(Fory, Compressors.Zstd) }

    // Map Codec

    /** Codec that converts [JsonObject] ↔ `Map<String, Any?>`. */
    val Map by lazy { MapJaversCodec() }
}
