package io.bluetape4k.javers.repository.jql

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.javers.examples.Address
import io.bluetape4k.javers.examples.Employee
import io.bluetape4k.javers.examples.Person
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JqlQueryExtensionsTest {

    private lateinit var javers: Javers

    @BeforeEach
    fun beforeEach() {
        javers = JaversBuilder.javers().build()
    }

    @Test
    fun `jql query extensions delegate shadow snapshot and change lookups`() {
        val bob = Person("bob", "Bob").apply {
            addresses += Address("Seoul", "A")
        }
        javers.commit("author", bob, mapOf("scope" to "initial"))
        bob.name = "Bob Changed"
        javers.commit("author", bob, mapOf("scope" to "update"))

        val query = queryByInstanceId<Person>("bob")

        query.findShadows<Person>(javers).size shouldBeGreaterOrEqualTo 2
        query.findShadowsAndStream<Person>(javers).use { stream ->
            stream.count() shouldBeGreaterOrEqualTo 2L
        }
        query.findShadowsAndSequence<Person>(javers).toList().size shouldBeGreaterOrEqualTo 2
        query.findSnapshots(javers).size shouldBeGreaterOrEqualTo 2
        query.findChanges(javers).size shouldBeGreaterOrEqualTo 1
    }

    @Test
    fun `query builder extensions cover instance class value object and multi-class variants`() {
        val bob = Person("bob", "Bob").apply {
            addresses += Address("Seoul", "A")
        }
        val alice = Person("alice", "Alice").apply {
            addresses += Address("Busan", "B")
        }
        val employee = Employee("tom").apply {
            primaryAddress = Address("Seoul", "HQ")
        }

        javers.commit("author", bob)
        javers.commit("author", alice)
        javers.commit("author", employee)

        javers.findSnapshots(queryAnyDomainObject()).size shouldBeGreaterOrEqualTo 5
        javers.findSnapshots(queryByInstance(bob)) shouldHaveSize 1
        javers.findSnapshots(queryByInstanceId<Person>("alice")) shouldHaveSize 1
        javers.findChanges(queryByValueObject<Person>("addresses/0")).shouldNotBeEmpty()
        javers.findChanges(queryByValueObjectId<Person>("bob", "addresses/0")).shouldNotBeEmpty()
        javers.findSnapshots(queryByClass<Employee>()).size shouldBeGreaterOrEqualTo 1
        javers.findSnapshots(queryByClasses(listOf(Person::class.java))).size shouldBeGreaterOrEqualTo 2
        javers.findSnapshots(queryByClasses(Person::class.java, Employee::class.java)).size shouldBeGreaterOrEqualTo 3
        javers.findSnapshots(queryByClasses(listOf(Person::class))).size shouldBeGreaterOrEqualTo 2
        javers.findSnapshots(queryByClasses(Person::class, Employee::class)).size shouldBeGreaterOrEqualTo 3
    }
}
