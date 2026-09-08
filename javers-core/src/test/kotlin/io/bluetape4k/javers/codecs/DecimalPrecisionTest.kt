package io.bluetape4k.javers.codecs

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.io.serializer.BinarySerializers
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.annotation.Id
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class DecimalPrecisionTest {

    private val values = listOf(
        "1234567890.1234567890123456789", "9223372036854775808", "-9876543210.123456789",
        "0.0000", "1.2300", "1E+30", "42.00",
    ).map(::BigDecimal)

    private fun input() = JsonObject().apply {
        add("values", JsonArray().apply { values.forEach { add(it) } })
        add("nested", JsonObject().apply { addProperty("amount", values.first()) })
    }

    private val codecs = listOf(
        JaversCodecs.Fory, JaversCodecs.Kryo,
        JaversCodecs.DeflateFory, JaversCodecs.GZipFory, JaversCodecs.LZ4Fory,
        JaversCodecs.SnappyFory, JaversCodecs.ZstdFory,
        JaversCodecs.DeflateKryo, JaversCodecs.GZipKryo, JaversCodecs.LZ4Kryo,
        JaversCodecs.SnappyKryo, JaversCodecs.ZstdKryo,
    )

    @TestFactory
    fun `binary codec은 중첩 decimal의 수치와 scale을 보존한다`() = codecs.mapIndexed { index, codec ->
        dynamicTest("decimal codec $index") {
            val restored = codec.decode(codec.encode(input())).shouldNotBeNull()
            restored.getAsJsonArray("values").map { it.asBigDecimal } shouldBeEqualTo values
            restored.getAsJsonObject("nested").get("amount").asBigDecimal shouldBeEqualTo values.first()
            restored.getAsJsonArray("values").all { it.asJsonPrimitive.isNumber }.shouldBeTrue()
        }
    }

    @Test
    fun `Map codec은 decimal 표현을 보존한다`() {
        val restored = JaversCodecs.Map.decode(JaversCodecs.Map.encode(input())).shouldNotBeNull()
        restored.getAsJsonArray("values").map { it.asBigDecimal } shouldBeEqualTo values
    }

    @Test
    fun `이전 Long Double map payload를 계속 읽는다`() {
        val legacy = linkedMapOf<String, Any?>("integer" to 42L, "decimal" to 1.25)
        listOf(BinarySerializers.Fory, BinarySerializers.Kryo).forEach { serializer ->
            val restored = BinaryJaversCodec(serializer).decode(serializer.serialize(legacy)).shouldNotBeNull()
            restored.get("integer").asLong shouldBeEqualTo 42L
            restored.get("decimal").asBigDecimal shouldBeEqualTo BigDecimal("1.25")
        }
    }

    @Test
    fun `실제 JaVers entity JSON도 decimal을 보존한다`() {
        val javers = JaversBuilder.javers().build()
        val entity = DecimalEntity(1L, values.first())
        val json = javers.jsonConverter.toJson(entity)
        val input = JaversCodecs.String.decode(json).shouldNotBeNull()
        codecs.take(2).forEach { codec ->
            val restored = codec.decode(codec.encode(input)).shouldNotBeNull()
            javers.jsonConverter.fromJson(restored, DecimalEntity::class.java).amount shouldBeEqualTo entity.amount
        }
    }

    class DecimalEntity(@Id val id: Long, val amount: BigDecimal)
}
