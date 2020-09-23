package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.aggregation.countItem
import xyz.magentaize.dynamicdata.domain.Person
import xyz.magentaize.dynamicdata.list.test.ChangeSetAggregator
import xyz.magentaize.dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.subjects.BehaviorSubject
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class FilterWithObservable : FilterBaseFixture() {
    override val filter: BehaviorSubject<(Person) -> Boolean> =
        BehaviorSubject.createDefault { it.age > 20 }

    override val result: ChangeSetAggregator<Person> =
        source.connect().filterItem(filter).asAggregator()

    @Test
    fun removeFilter() {
        val p = Person("P1", 1)
        source.add(p)
        result.data.size shouldBeEqualTo 0

        filter.onNext { it.age >= 1 }
        result.data.size shouldBeEqualTo 1

        source.remove(p)
        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun removeFilteredRange() {
        val people = (1..10).map { Person("P$it", it) }.toList()
        source.addRange(people)
        result.data.size shouldBeEqualTo 0

        filter.onNext { it.age > 5 }
        result.data.size shouldBeEqualTo 5

        source.removeRange(5, 5)
        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun chainFilters() {
        val filter2 = BehaviorSubject.createDefault<(Person) -> Boolean> { it.age > 20 }
        val stream = source.connect().filterItem(filter).filterItem(filter2)
        val capture = mutableListOf<Int>()
        stream.countItem().subscribe { capture.add(it) }

        val p = Person("P1", 30)
        source.add(p)
        p.age = 10
        filter.onNext(filter.value)
        capture shouldBeEqualTo listOf(1, 0)
    }
}
