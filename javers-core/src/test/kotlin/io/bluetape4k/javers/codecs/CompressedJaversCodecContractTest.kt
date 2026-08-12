package io.bluetape4k.javers.codecs

import com.github.benmanes.caffeine.cache.Cache
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.javers.repository.caffeine.CaffeineCdoSnapshotRepository
import io.bluetape4k.javers.repository.jql.queryByInstanceId
import org.javers.core.JaversBuilder
import org.javers.core.model.SnapshotEntity
import org.junit.jupiter.api.Test

class CompressedJaversCodecContractTest {

    @Test
    fun `compressed string codec returns null for malformed payload`() {
        val codec = CompressibleStringJaversCodec(StringJaversCodec(), Compressors.GZip)

        codec.decode("not-base64").shouldBeNull()
    }

    @Test
    fun `compressed binary codec returns null for malformed payload`() {
        val codec = CompressibleBinaryJaversCodec(
            BinaryJaversCodec(BinarySerializers.Kryo),
            Compressors.GZip,
        )

        codec.decode(byteArrayOf(0x01, 0x02, 0x03)).shouldBeNull()
    }

    @Test
    fun `repository skips malformed compressed snapshot`() {
        val repository = CaffeineCdoSnapshotRepository(
            CompressibleStringJaversCodec(StringJaversCodec(), Compressors.GZip),
        )
        val javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()
        val entity = SnapshotEntity(307).apply { intProperty = 1 }

        javers.commit("codec-contract", entity)
        repository.snapshotCache().asMap().values.single().add("not-base64")

        javers.findSnapshots(queryByInstanceId<SnapshotEntity>(307)) shouldHaveSize 1
    }

    @Suppress("UNCHECKED_CAST")
    private fun CaffeineCdoSnapshotRepository.snapshotCache(): Cache<String, MutableList<String>> {
        val delegateField = CaffeineCdoSnapshotRepository::class.java
            .getDeclaredField("snapshotCache\$delegate")
            .apply { isAccessible = true }
        return (delegateField.get(this) as Lazy<Cache<String, MutableList<String>>>).value
    }
}
