package dynamicdata.list

import dynamicdata.domain.Person
import dynamicdata.domain.PersonWithGender
import dynamicdata.list.test.asAggregator
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class TransformFixture {
    val factory = { it: Person ->
        val gender = if (it.age % 2 == 0) "M" else "F"
        PersonWithGender(it, gender)
    }
    val source = SourceList<Person>()
    val result = source.connect().transform(factory).asAggregator()
    val key = "Adult1"

    @Test
    fun add() {
        val p = Person(key, 50)
        source.add(p)

        result.messages.size shouldBeEqualTo 1
        result.data.size shouldBeEqualTo 1
        result.data.items.first() shouldBeEqualTo factory(p)
    }

    @Test
    fun remove() {
        val p = Person(key, 50)
        source.add(p)
        source.remove(p)

        result.messages.size shouldBeEqualTo 2
        result.messages[0].adds shouldBeEqualTo 1
        result.messages[1].removes shouldBeEqualTo 1
        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun removeWithoutIndex() {
        val p = Person(key, 50)
        val result = source.connect().removeIndex().transform(factory).asAggregator()
        source.add(p)
        source.remove(p)

        result.messages.size shouldBeEqualTo 2
        result.messages[0].adds shouldBeEqualTo 1
        result.messages[1].removes shouldBeEqualTo 1
        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun update() {
        val new = Person(key, 50)
        val up = Person(key, 51)
        source.add(new)
        source.add(up)

        result.messages.size shouldBeEqualTo 2
        result.messages[0].adds shouldBeEqualTo 1
        result.messages[0].replaced shouldBeEqualTo 0
    }

    @Test
    fun batchOfUniqueUpdates() {
        val people = (1..100).map { Person("P$it", it) }.toList()
        source.addRange(people)

        result.messages.size shouldBeEqualTo 1
        result.messages[0].adds shouldBeEqualTo 100
        result.data.items.sortedBy { it.age }.toList() shouldBeEqualTo people.map(factory).sortedBy { it.age }.toList()
    }

    @Test
    fun sameKeyChanges() {
        val people = (1..10).map { Person("P", it) }.toList()
        source.addRange(people)

        result.messages.size shouldBeEqualTo 1
        result.messages[0].adds shouldBeEqualTo 10
        result.data.size shouldBeEqualTo 10
    }

    @Test
    fun clear() {
        val people = (1..100).map { Person("P$it", it) }.toList()
        source.addRange(people)
        source.clear()

        result.messages.size shouldBeEqualTo 2
        result.messages[0].adds shouldBeEqualTo 100
        result.messages[1].removes shouldBeEqualTo 100
        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun transformToNull() {
        val result = source.connect().transform<Person, PersonWithGender?> { null }.asAggregator()
        source.add(Person(key, 50))

        result.messages.size shouldBeEqualTo 1
        result.data.size shouldBeEqualTo 1
        result.data.items.first() shouldBeEqualTo null
    }
}
