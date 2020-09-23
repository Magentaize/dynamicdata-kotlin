package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.domain.Person
import xyz.magentaize.dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.subjects.BehaviorSubject
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class FilterControllerBaseFixtureWithClearAndReplace : FilterBaseFixture() {
    override val filter: BehaviorSubject<(Person) -> Boolean> =
        BehaviorSubject.createDefault { it.age > 20 }

    override val result =
        source.connect().filterItem(filter, ListFilterPolicy.ClearAndReplace).asAggregator()

    @Test
    fun changeFilter() {
        val people = (1..100).map { Person("P$it", it) }.toList()
        source.addRange(people)
        result.data.size shouldBeEqualTo 80

        filter.onNext { it.age <= 50 }
        result.data.size shouldBeEqualTo 50
        result.messages.size shouldBeEqualTo 2
        result.data.items.all { it.age <= 50 } shouldBeEqualTo true
    }

    @Test
    fun veryLargeDataSet() {
        val filter = BehaviorSubject.createDefault<(Int) -> Boolean> { false }
        val source = SourceList<Int>()
        val result = source.connect().filterItem(filter, ListFilterPolicy.ClearAndReplace).asObservableList()
        source.addRange(1..250000)
        filter.onNext { true }
        filter.onNext { false }
    }

    @Test
    fun reevaluateFilter() {
        val people = (1..100).map { Person("P$it", it) }.toList()
        source.addRange(people)
        result.data.size shouldBeEqualTo 80

        people.forEach { it.age += 10 }
        filter.onNext { it.age > 20 }
        result.data.size shouldBeEqualTo 90
        result.messages.size shouldBeEqualTo 2
        result.messages[1].removes shouldBeEqualTo 80
        result.messages[1].adds shouldBeEqualTo 90

        people.forEach { it.age -= 10 }
        filter.onNext { it.age > 20 }
        result.data.size shouldBeEqualTo 80
        result.messages.size shouldBeEqualTo 3
    }
}
