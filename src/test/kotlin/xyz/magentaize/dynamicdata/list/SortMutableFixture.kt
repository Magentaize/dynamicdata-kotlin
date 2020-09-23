package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.domain.Person
import xyz.magentaize.dynamicdata.domain.RandomPersonGenerator
import xyz.magentaize.dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class SortMutableFixture {
    val source = SourceList<Person>()
    val comparer = compareBy<Person> { it.age }.thenBy { it.name }
    val changeComparer = BehaviorSubject.createDefault(comparer)
    val resort = PublishSubject.create<Unit>()
    val result = source.connect().sort(changeComparer, resetThreshold = 25, resort = resort).asAggregator()

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
        val p = Person("\u0000A", 10000)
        source.add(p)

        result.data.size shouldBeEqualTo 101
        result.data.items.last() shouldBe p
    }

    @Test
    fun replace() {
        val people = RandomPersonGenerator.take(100).toList()
        source.addRange(people)
        val p = Person("\u0000A", 999)
        source.replaceAt(10, p)

        result.data.size shouldBeEqualTo 100
        result.data.items.last() shouldBe p
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
    fun resortOnInlineChanges() {
        val people = RandomPersonGenerator.take(10).toList()
        source.addRange(people)
        people[0].age = -1
        people[1].age = -10
        people[2].age = -12
        people[3].age = -5
        people[4].age = -7
        people[5].age = -6

        val comparer = compareByDescending<Person> { it.age }.thenBy { it.name }
        changeComparer.onNext(comparer)

        result.data.toList() shouldBeEqualTo people.sortedWith(comparer).toList()
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

    @Test
    fun resort() {
        val people = RandomPersonGenerator.take(100).toList()
        source.addRange(people)
        people.shuffled().forEachIndexed { index, person -> person.age = index }
        resort.onNext(Unit)

        result.data.size shouldBeEqualTo 100
        result.data.toList() shouldBeEqualTo people.sortedWith(comparer).toList()
    }

    @Test
    fun changeComparer() {
        val people = RandomPersonGenerator.take(100).toList()
        source.addRange(people)
        val comparer = compareBy<Person> { it.name }.thenBy { it.age }
        changeComparer.onNext(comparer)

        result.data.size shouldBeEqualTo 100
        result.data.toList() shouldBeEqualTo people.sortedWith(comparer).toList()
    }

    @Test
    fun updateMoreThanThreshold() {
        val allPeople = RandomPersonGenerator.take(1100).toList()
        val people = allPeople.take(100).toList()
        source.addRange(people)
        result.data.size shouldBeEqualTo 100

        val morePeople = allPeople.drop(100).toList()
        source.addRange(morePeople)
        result.data.size shouldBeEqualTo 1100
        result.data.toList() shouldBeEqualTo people.union(morePeople).sortedWith(comparer).toList()
        result.messages.size shouldBeEqualTo 2
        val last = result.messages.last()
        last.first().range.size shouldBeEqualTo 100
        last.first().reason shouldBeEqualTo ListChangeReason.Clear
        last.last().range.size shouldBeEqualTo 1100
        last.last().reason shouldBeEqualTo ListChangeReason.AddRange
    }
}
