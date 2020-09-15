package dynamicdata.list

import dynamicdata.domain.Person
import dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.subjects.PublishSubject
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class GroupImmutableFixture {
    val source = SourceList<Person>()
    val regrouper = PublishSubject.create<Unit>()
    val result = source.connect().groupWithImmutableState({ it.age }, regrouper).asAggregator()

    @Test
    fun add() {
        source.add(Person("P1", 20))

        result.data.size shouldBeEqualTo 1
        result.messages.first().adds shouldBeEqualTo 1
    }

    @Test
    fun updatesArePermissible() {
        source.add(Person("P1", 20))
        source.add(Person("P2", 20))

        result.data.size shouldBeEqualTo 1
        result.messages.first().adds shouldBeEqualTo 1
        result.messages.drop(1).first().replaced shouldBeEqualTo 1
        result.data.items.first().size shouldBeEqualTo 2
    }

    @Test
    fun updateAnItemWillChangedThegroup() {
        val p = Person("P1", 20)
        source.add(p)
        source.replace(p, Person("P1", 21))

        result.data.size shouldBeEqualTo 1
        result.messages.first().adds shouldBeEqualTo 1
        result.messages.drop(1).first().adds shouldBeEqualTo 1
        result.messages.drop(1).first().removes shouldBeEqualTo 1
        val group = result.data.items.first()
        group.size shouldBeEqualTo 1
        group.key shouldBeEqualTo 21
    }

    @Test
    fun remove() {
        val p = Person("P1", 20)
        source.add(p)
        source.remove(p)

        result.messages.size shouldBeEqualTo 2
        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun firesManyValueForBatchOfDifferentAdds() {
        source.edit {
            (0..3).forEach { n -> it.add(Person("P$n", 20 + n)) }
        }

        result.data.size shouldBeEqualTo 4
        result.messages.size shouldBeEqualTo 1
        result.messages.first().size shouldBeEqualTo 1
        result.messages.first().forEach {
            it.reason shouldBeEqualTo ListChangeReason.AddRange
        }
    }

    @Test
    fun firesOnlyOnceForABatchOfUniqueValues() {
        source.edit {
            (0..3).forEach { n -> it.add(Person("P$n", 20)) }
        }

        result.messages.size shouldBeEqualTo 1
        result.messages.first().adds shouldBeEqualTo 1
        result.data.items.first().size shouldBeEqualTo 4
    }

    @Test
    fun changeMultipleGroups() {
        val people = (1..10000).map { Person("P$it", it % 10) }.toList()
        source.addRange(people)
        people.groupBy { it.age }
            .forEach { (k, u) ->
                val grp = result.data.items.first { g -> g.key == k }
                grp.items.toList() shouldBeEqualTo u.toList()
            }

        source.removeAll(people.take(15))
        people.drop(15)
            .groupBy { it.age }
            .forEach { (k, u) ->
                val grp = result.data.items.first { g -> g.key == k }
                grp.items.toList() shouldBeEqualTo u.toList()
            }

        result.messages.size shouldBeEqualTo 2
        result.messages.first().adds shouldBeEqualTo 10
        result.messages.drop(1).first().replaced shouldBeEqualTo 10
    }

    @Test
    fun reevaluate() {
        val people = (1..10).map { Person("P$it", it % 2) }.toList()
        source.addRange(people)
        result.messages.size shouldBeEqualTo 1

        people.forEach { it.age++ }

        regrouper.onNext(Unit)

        people.groupBy { it.age }
            .forEach { (k, u) ->
                val grp = result.data.items.first { g -> g.key == k }
                grp.items.toList() shouldBeEqualTo u.toList()
            }

        result.data.size shouldBeEqualTo 2
        result.messages.size shouldBeEqualTo 2
        val msg2 = result.messages.drop(1).first()
        msg2.removes shouldBeEqualTo 1
        msg2.replaced shouldBeEqualTo 1
        msg2.adds shouldBeEqualTo 1
    }
}
