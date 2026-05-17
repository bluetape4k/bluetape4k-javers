package io.bluetape4k.javers

import io.bluetape4k.logging.KLogging
import org.javers.core.Javers
import org.javers.core.metamodel.type.TypeMapper
import org.javers.shadow.ShadowFactory
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton provider that creates and caches [ShadowFactory] instances per [Javers] instance.
 *
 * ## Behavior / Contract
 * - A [ShadowFactory] is cached in a [ConcurrentHashMap] per [Javers] instance.
 * - The internal [TypeMapper] is extracted via reflection on the first call.
 * - Throws [IllegalStateException] if Javers internals change and the `typeMapper` field is not found.
 *
 * ```kotlin
 * val factory = ShadowProvider.getShadowFactory(javers)
 * val shadow = factory.createShadow(snapshot, metadata, null)
 * ```
 */
object ShadowProvider: KLogging() {

    private val typeMappers = ConcurrentHashMap<Javers, TypeMapper>()
    private val shadowFactories = ConcurrentHashMap<Javers, ShadowFactory>()

    /**
     * Returns the [ShadowFactory] corresponding to the given [javers] instance.
     *
     * ## Behavior / Contract
     * - Always returns the same [ShadowFactory] for the same [Javers] instance.
     * - On the first call, extracts the internal [TypeMapper] from [Javers] via reflection.
     *
     * @param javers the [Javers] instance whose shadow factory is needed
     */
    fun getShadowFactory(javers: Javers): ShadowFactory {
        return shadowFactories.computeIfAbsent(javers) {
            ShadowFactory(javers.jsonConverter, getTypeMapper(javers))
        }
    }

    /**
     * Extracts the internal [TypeMapper] from [Javers] via reflection.
     *
     * @throws IllegalStateException if the `typeMapper` field cannot be found in the Javers class
     */
    private fun getTypeMapper(javers: Javers): TypeMapper {
        return typeMappers.computeIfAbsent(javers) {
            val field: Field = javers.javaClass.declaredFields.find { it.name == "typeMapper" }
                ?: error("Javers internals changed: 'typeMapper' field not found in ${javers.javaClass.name}")
            field.isAccessible = true
            field.get(javers) as TypeMapper
        }
    }
}
