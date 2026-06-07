package io.bluetape4k.javers.repository

import com.google.gson.JsonObject
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.codec.Base58
import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.cache2k.Cache2KCdoSnapshotRepository
import io.bluetape4k.javers.repository.caffeine.CaffeineCdoSnapshotRepository
import io.bluetape4k.javers.repository.jcache.JCacheCdoSnapshotRepository
import org.javers.core.JaversBuilder
import org.javers.core.model.SnapshotEntity
import org.javers.repository.jql.QueryBuilder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class CdoSnapshotRepositoryCodecContractTest {

    @ParameterizedTest(name = "{0} default codec")
    @MethodSource("defaultStringRepositories")
    fun `string repositories default to LZ4 string codec`(
        name: String,
        factory: DefaultStringRepositoryFactory,
    ) {
        val repository = factory.create()

        repository.codec() shouldBeEqualTo JaversCodecs.LZ4String

        assertSnapshotRoundTrip(repository, name.hashCode())
    }

    @ParameterizedTest(name = "{0} custom codec")
    @MethodSource("stringRepositories")
    fun `string repositories round trip snapshots with custom codec`(
        name: String,
        factory: StringRepositoryFactory,
    ) {
        val codec = TrackingStringCodec()
        val repository = factory.create(codec)

        assertSnapshotRoundTrip(repository, name.hashCode())

        codec.encodeCount shouldBeGreaterThan 0
        codec.decodeCount shouldBeGreaterThan 0
    }

    private fun assertSnapshotRoundTrip(
        repository: AbstractCdoSnapshotRepository<String>,
        id: Int,
    ) {
        val javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()
        val entity = SnapshotEntity(id).apply { intProperty = 1 }

        javers.commit("codec-contract", entity)
        entity.intProperty = 2
        javers.commit("codec-contract", entity)

        val snapshots = javers.findSnapshots(QueryBuilder.byInstanceId(id, SnapshotEntity::class.java).build())

        snapshots.size shouldBeEqualTo 2
        snapshots[0].version shouldBeEqualTo 2L
        snapshots[0].getPropertyValue("intProperty") shouldBeEqualTo 2
        snapshots[1].version shouldBeEqualTo 1L
        snapshots[1].getPropertyValue("intProperty") shouldBeEqualTo 1
    }

    private fun AbstractCdoSnapshotRepository<*>.codec(): Any {
        val field = AbstractCdoSnapshotRepository::class.java.getDeclaredField("codec")
        field.isAccessible = true
        return field.get(this)
    }

    private class TrackingStringCodec: JaversCodec<String> {
        var encodeCount: Int = 0
            private set
        var decodeCount: Int = 0
            private set

        override fun encode(jsonElement: JsonObject): String {
            encodeCount++
            return JaversCodecs.String.encode(jsonElement)
        }

        override fun decode(encodedData: String): JsonObject? {
            decodeCount++
            return JaversCodecs.String.decode(encodedData)
        }
    }

    fun interface StringRepositoryFactory {
        fun create(codec: JaversCodec<String>): AbstractCdoSnapshotRepository<String>
    }

    fun interface DefaultStringRepositoryFactory {
        fun create(): AbstractCdoSnapshotRepository<String>
    }

    companion object {
        @JvmStatic
        fun defaultStringRepositories(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "Cache2k",
                DefaultStringRepositoryFactory { Cache2KCdoSnapshotRepository() },
            ),
            Arguments.of(
                "Caffeine",
                DefaultStringRepositoryFactory { CaffeineCdoSnapshotRepository() },
            ),
            Arguments.of(
                "JCache",
                DefaultStringRepositoryFactory {
                    JCacheCdoSnapshotRepository(
                        "codec-contract-default-${Base58.randomString(12)}",
                        JCaching.Caffeine.cacheManager,
                    )
                },
            ),
        )

        @JvmStatic
        fun stringRepositories(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "Cache2k",
                StringRepositoryFactory { codec -> Cache2KCdoSnapshotRepository(codec) },
            ),
            Arguments.of(
                "Caffeine",
                StringRepositoryFactory { codec -> CaffeineCdoSnapshotRepository(codec) },
            ),
            Arguments.of(
                "JCache",
                StringRepositoryFactory { codec ->
                    JCacheCdoSnapshotRepository(
                        "codec-contract-${Base58.randomString(12)}",
                        JCaching.Caffeine.cacheManager,
                        codec,
                    )
                },
            ),
        )
    }
}
