package io.bluetape4k.javers

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import org.javers.core.JaversBuilder
import org.javers.shadow.ShadowFactory
import org.junit.jupiter.api.Test
import java.util.WeakHashMap

class ShadowProviderTest {

    @Test
    fun `same Javers instance reuses factory and many instances remain recoverable`() {
        val javers = JaversBuilder.javers().build()
        ShadowProvider.getShadowFactory(javers) shouldBeEqualTo ShadowProvider.getShadowFactory(javers)

        val factories = (1..32).map { ShadowProvider.getShadowFactory(JaversBuilder.javers().build()) }
        factories.size shouldBeGreaterOrEqualTo 32
        factories.first().shouldBeInstanceOf<ShadowFactory>()
    }

    @Test
    fun `cache uses weak keys for Javers lifecycle`() {
        val field = ShadowProvider::class.java.getDeclaredField("shadowFactories").apply { isAccessible = true }
        field.get(ShadowProvider).shouldBeInstanceOf<WeakHashMap<*, *>>()
    }
}
