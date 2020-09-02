package dynamicdata.kernel

import dynamicdata.cache.Change
import dynamicdata.cache.ChangeReason
import dynamicdata.domain.Person
import org.amshove.kluent.*
import kotlin.test.Test
import java.lang.IllegalArgumentException

internal class DistinctUpdateFixture {
    @Test
    fun add() {
        val person = Person("Person", 10)
        val update = Change<Person, Person>(ChangeReason.Add, person, person)

        update.key shouldBe person
        update.reason shouldBe ChangeReason.Add
        update.current shouldBe person
        update.previous shouldBe null
    }

    @Test
    fun remove() {
        val person = Person("Person", 10)
        val update = Change<Person, Person>(ChangeReason.Remove, person, person)

        update.key shouldBe person
        update.reason shouldBe ChangeReason.Remove
        update.current shouldBe person
        update.previous shouldBe null
    }

    @Test
    fun update() {
        val current = Person("Person", 10)
        val previous = Person("Person", 9)
        val update = Change<Person, Person>(ChangeReason.Update, current, current, previous)

        update.key shouldBe current
        update.reason shouldBe ChangeReason.Update
        update.current shouldBe current
        update.previous `should not be` null
        update.previous shouldBe previous
    }

    @Test
    fun updateWillThrowIfNoPreviousValueIsSupplied() {
        val current = Person("Person", 10)

        invoking { Change(ChangeReason.Update, current, current) }
            .shouldThrow(IllegalArgumentException::class)
    }
}
