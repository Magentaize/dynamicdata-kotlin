package dynamicdata.list

import dynamicdata.domain.Person
import dynamicdata.list.test.asAggregator
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class FilterFixture {
    private val source = SourceList<Person>()
    private val result = source.connect { it.age > 20 }.asAggregator()

    @Test
    fun addMatched() {
        val p = Person("Adult1", 50)
        source.add(p)

        result.messages.size shouldBeEqualTo 1
        result.data.size shouldBeEqualTo 1
        result.data.items.first() shouldBe p
    }

    @Test
    fun replaceWithMatch() {
        val add = (1..100).map { Person("P$it", it) }.toList()
        source.addRange(add)
        source.replaceAt(0, Person("Adult1", 50))

        result.data.size shouldBeEqualTo 81
    }

    @Test
    fun replaceWithNonMatch() {
        val add = (1..100).map { Person("P$it", it) }.toList()
        source.addRange(add)
        source.replaceAt(50, Person("Adult1", 1))

        result.data.size shouldBeEqualTo 79
    }

    @Test
    fun addRange() {
        val add = (1..100).map { Person("P$it", it) }.toList()
        source.addRange(add)

        result.messages.size shouldBeEqualTo 1
        result.messages[0].first().reason shouldBeEqualTo ListChangeReason.AddRange
        result.data.size shouldBeEqualTo 80
    }

    @Test
    fun clear() {
        val add = (1..100).map { Person("P$it", it) }.toList()
        source.addRange(add)
        source.clear()

        result.messages.size shouldBeEqualTo 2
        result.messages[0].first().reason shouldBeEqualTo ListChangeReason.AddRange
        result.messages[1].first().reason shouldBeEqualTo ListChangeReason.Clear
        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun addNotMatched() {
        val p = Person("Adult1", 10)
        source.add(p)

        result.messages.size shouldBeEqualTo 0
        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun addNotMatchedAndUpdateMatched() {
        val key = "Adult1"
        val notmatched = Person(key, 19)
        val matched = Person(key, 21)

        source.edit {
            it.add(notmatched)
            it.add(matched)
        }

        result.messages.size shouldBeEqualTo 1
        result.messages[0].first().range.first() shouldBe matched
        result.data.items.first() shouldBe matched
    }

    @Test
    fun attemptedRemovalOfANonExistentKeyWillBeIgnored() {
        source.remove(Person("anyone", 1))
        result.messages.size shouldBeEqualTo 0
    }

    @Test
    fun batchOfUniqueUpdates() {
        val people = (1..100).map { Person("Name$it", it) }.toList()
        source.addRange(people)

        result.messages.size shouldBeEqualTo 1
        result.messages[0].adds shouldBeEqualTo 80

        val filtered = people.filter { it.age > 20 }.sortedBy { it.age }
        result.data.items.sortedBy { it.age }.toList() shouldBeEqualTo filtered.toList()
    }

    @Test
    fun batchRemoves() {
        val people = (1..100).map { Person("Name$it", it) }.toList()
        source.addRange(people)
        source.clear()

        result.messages.size shouldBeEqualTo 2
        result.messages[0].adds shouldBeEqualTo 80
        result.messages[1].removes shouldBeEqualTo 80
        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun batchSuccessiveUpdates() {
        val people = (1..100).map { Person("Name$it", it) }.toList()
        people.forEach {
            val p1 = it
            source.add(p1)
        }

        result.messages.size shouldBeEqualTo 80
        result.data.size shouldBeEqualTo 80

        val filtered = people.filter { it.age > 20 }.sortedBy { it.age }
        result.data.items.sortedBy { it.age } shouldBeEqualTo filtered.toList()
    }

    @Test
    fun clear1() {
        val people = (1..100).map { Person("Name$it", it) }.toList()
        source.addRange(people)
        source.clear()

        result.messages.size shouldBeEqualTo 2
        result.messages[0].adds shouldBeEqualTo 80
        result.messages[1].removes shouldBeEqualTo 80
        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun remove() {
        val key = "Adult1"
        val p = Person(key, 50)
        source.add(p)
        source.remove(p)

        result.messages.size shouldBeEqualTo 2
        result.messages[0].adds shouldBeEqualTo 1
        result.messages[1].removes shouldBeEqualTo 1
        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun sameKeyChanges() {
        val key = "Adult1"
        val p = Person(key, 53)
        source.edit {
            it.add(Person(key, 50))
            it.add(Person(key, 52))
            it.add(p)
            it.remove(p)
        }

        result.messages.size shouldBeEqualTo 1
        result.messages[0].adds shouldBeEqualTo 3
        result.messages[0].replaced shouldBeEqualTo 0
        result.messages[0].removes shouldBeEqualTo 1
    }

    @Test
    fun updateNotMatched() {
        val key = "Adult1"
        val new = Person(key, 10)
        val updated = Person(key, 11)
        source.add(new)
        source.add(updated)

        result.messages.size shouldBeEqualTo 0
        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun addSubscribeRemove() {
        val people = (1..100).map { Person("Name$it", it) }.toList()
        val source = SourceList<Person>()
        source.addRange(people)
        val result = source.connect { it.age > 20 }.asAggregator()
        source.removeAll(people.filter { it.age % 2 == 0 })

        result.data.size shouldBeEqualTo 40
    }
}
