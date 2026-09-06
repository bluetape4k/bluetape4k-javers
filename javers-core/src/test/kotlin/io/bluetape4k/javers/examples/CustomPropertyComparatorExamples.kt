package io.bluetape4k.javers.examples

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.assertions.shouldHaveSize
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.diff.changetype.PropertyChangeMetadata
import org.javers.core.diff.changetype.ValueChange
import org.javers.core.diff.custom.CustomPropertyComparator
import org.javers.core.metamodel.annotation.Id
import org.javers.core.metamodel.annotation.TypeName
import org.javers.core.metamodel.property.Property
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.util.Optional

class CustomPropertyComparatorExamples {

    private val javers: Javers = JaversBuilder.javers()
        .registerCustomType(String::class.java, StringPropertyComparator())
        .build()

    @Test
    fun `다형성 객체에서 한쪽에만 존재하는 String 속성도 comparator로 비교합니다`() {
        val legacy = LegacyDocument(
            id = 1,
            title = "JaVers",
            legacyLabel = "legacy",
        )
        val current = CurrentDocument(
            id = 1,
            title = "JaVers",
            currentLabel = "current",
        )

        val diff = javers.compare(legacy, current)
        val changes = diff.changes.filterIsInstance<ValueChange>()

        changes shouldHaveSize 2
        changes.map { it.propertyName } shouldContainSame listOf("legacyLabel", "currentLabel")

        val changesByProperty = changes.associateBy { it.propertyName }
        changesByProperty.getValue("legacyLabel").left shouldBeEqualTo "legacy"
        changesByProperty.getValue("legacyLabel").right shouldBeEqualTo null
        changesByProperty.getValue("currentLabel").left shouldBeEqualTo null
        changesByProperty.getValue("currentLabel").right shouldBeEqualTo "current"
    }

    private class StringPropertyComparator : CustomPropertyComparator<String, ValueChange>, Serializable {
        override fun equals(a: String?, b: String?): Boolean = a == b

        override fun toString(value: String?): String = value.orEmpty()

        override fun compare(
            left: String?,
            right: String?,
            metadata: PropertyChangeMetadata,
            property: Property,
        ): Optional<ValueChange> =
            if (equals(left, right)) {
                Optional.empty()
            } else {
                Optional.of(ValueChange(metadata, left, right))
            }
    }

    @TypeName("PolymorphicDocument")
    private data class LegacyDocument(
        @Id val id: Int,
        val title: String,
        val legacyLabel: String,
    ) : Serializable

    @TypeName("PolymorphicDocument")
    private data class CurrentDocument(
        @Id val id: Int,
        val title: String,
        val currentLabel: String,
    ) : Serializable
}
