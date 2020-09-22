package dynamicdata.list

import dynamicdata.domain.Person
import dynamicdata.domain.RandomPersonGenerator
import dynamicdata.list.test.asAggregator
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class SortFixture {
    val source = SourceList<Person>()
    val comparer = compareBy<Person> { it.name }.thenBy { it.age }
    val result = source.connect().sort(comparer).asAggregator()

    @Test
    fun sortInitialBatch() {
        val people = RandomPersonGenerator.take(100).toList()
        source.addRange(people)

        result.data.size shouldBeEqualTo 100
        result.data.items.toList() shouldBeEqualTo people.sortedWith(comparer).toList()
    }

    @Test
    fun insert() {
        val people = RandomPersonGenerator.take(100).toList()
        source.addRange(people)
        val p = Person("\u0000A", 99)
        source.add(p)

        result.data.size shouldBeEqualTo 101
        result.data.items.first() shouldBe p
    }

    @Test
    fun replace() {
        val people = RandomPersonGenerator.take(100).toList()
        source.addRange(people)
        val p = Person("\u0000A", 99)
        source.replaceAt(10, p)

        result.data.size shouldBeEqualTo 100
        result.data.items.first() shouldBe p
    }

    @Test
    fun remove() {
        val people = RandomPersonGenerator.take(100).toMutableList()
        source.addRange(people)
        val p = people.elementAt(20)
        people.removeAt(20)
        source.removeAt(20)

        result.data.size shouldBeEqualTo 99
        result.messages.size shouldBeEqualTo 2
        result.messages[1].first().item.current shouldBe p
        result.data.toList() shouldBeEqualTo people.sortedWith(comparer).toList()
    }

    @Test
    fun removeManyOrdered() {
        val people = RandomPersonGenerator.take(100).toMutableList()
        source.addRange(people)
        source.removeAll(people.sortedWith(comparer).drop(10).take(90))

        result.data.size shouldBeEqualTo 10
        result.messages.size shouldBeEqualTo 2
        result.data.toList() shouldBeEqualTo people.sortedWith(comparer).take(10).toList()
    }

    @Test
    fun removeManyReverseOrdered() {
        val people = RandomPersonGenerator.take(100).toMutableList()
        source.addRange(people)
        source.removeAll(people.sortedWith(comparer.reversed()).drop(10).take(90))

        result.data.size shouldBeEqualTo 10
        result.messages.size shouldBeEqualTo 2
        result.data.toList() shouldBeEqualTo people.sortedWith(comparer.reversed()).take(10).reversed().toList()
    }

    @Test
    fun removeManyOdds() {
        val people = RandomPersonGenerator.take(100).toMutableList()
        source.addRange(people)
        val odd = people.filterIndexed { index, _ -> index % 2 == 1 }.toList()
        source.removeAll(odd)

        result.data.size shouldBeEqualTo 50
        result.messages.size shouldBeEqualTo 2
        result.data.toList() shouldBeEqualTo people.minus(odd).sortedWith(comparer).toList()
    }
}
