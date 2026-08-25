package io.bluetape4k.javers

import io.bluetape4k.logging.KLogging
import org.javers.core.Javers
import org.javers.core.metamodel.type.TypeMapper
import org.javers.shadow.ShadowFactory
import java.lang.reflect.Field
import java.util.WeakHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * [Javers] instance별 [ShadowFactory]를 생성하고 cache하는 singleton provider입니다.
 *
 * ## 동작 / 계약
 * - [Javers] instance마다 [ShadowFactory]를 weak-key cache에 저장합니다.
 * - cache value가 [Javers]를 강하게 참조하지 않으므로 instance 수명 종료 후 entry가 회수될 수 있습니다.
 * - cache는 lock으로 보호하며 같은 [Javers] instance에는 항상 같은 factory를 반환합니다.
 * - 최초 호출 시 reflection으로 내부 [TypeMapper]를 추출합니다.
 * - Javers 내부 구조가 바뀌어 `typeMapper` field를 찾을 수 없으면 [IllegalStateException]을 던집니다.
 *
 * ```kotlin
 * val factory = ShadowProvider.getShadowFactory(javers)
 * val shadow = factory.createShadow(snapshot, metadata, null)
 * ```
 */
object ShadowProvider: KLogging() {

    private val shadowFactories: MutableMap<Javers, ShadowFactory> = WeakHashMap()
    private val cacheLock = ReentrantLock()

    /**
     * 지정한 [javers] instance에 대응하는 [ShadowFactory]를 반환합니다.
     *
     * ## 동작 / 계약
     * - 같은 [Javers] instance에 대해서는 항상 같은 [ShadowFactory]를 반환합니다.
     * - 최초 호출 시 [Javers]에서 내부 [TypeMapper]를 reflection으로 추출합니다.
     *
     * @param javers shadow factory가 필요한 [Javers] instance입니다.
     */
    fun getShadowFactory(javers: Javers): ShadowFactory {
        return cacheLock.withLock {
            shadowFactories[javers] ?: ShadowFactory(javers.jsonConverter, getTypeMapper(javers)).also {
                shadowFactories[javers] = it
            }
        }
    }

    /**
     * [Javers]에서 내부 [TypeMapper]를 reflection으로 추출합니다.
     *
     * @throws IllegalStateException Javers class에서 `typeMapper` field를 찾을 수 없을 때 발생합니다.
     */
    private fun getTypeMapper(javers: Javers): TypeMapper {
        val field: Field = javers.javaClass.declaredFields.find { it.name == "typeMapper" }
            ?: error("Javers internals changed: 'typeMapper' field not found in ${javers.javaClass.name}")
        field.isAccessible = true
        return field.get(javers) as TypeMapper
    }
}
