package dynamicdata.list

import dynamicdata.domain.Person
import dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.kotlin.toObservable
import io.reactivex.rxjava3.schedulers.TestScheduler
import org.amshove.kluent.shouldBeEqualTo
import java.util.concurrent.TimeUnit
import kotlin.test.Test

internal class AutoRefreshFixture {
    @Test
    fun autoRefresh() {
        val items = (1..100).map { Person("Person$it", 1) }
        val list = SourceList<Person>()
        val results = list.connect().autoRefresh(Person::age).asAggregator()

        list.addRange(items)

        results.data.size shouldBeEqualTo 100
        results.messages.size shouldBeEqualTo 1

        items[0].age = 10
        results.data.size shouldBeEqualTo 100
        results.messages.size shouldBeEqualTo 2
        results.messages[1].first().reason shouldBeEqualTo ListChangeReason.Refresh

        //remove an item and check no change is fired
        val toRemove = items[1]

        list.remove(toRemove)
        results.data.size shouldBeEqualTo 99
        results.messages.size shouldBeEqualTo 3

        toRemove.age = 100
        results.messages.size shouldBeEqualTo 3

        //add it back in and check it updates
        list.add(toRemove)
        results.messages.size shouldBeEqualTo 4

        toRemove.age = 101
        results.messages.size shouldBeEqualTo 5
        results.messages.last().first().reason shouldBeEqualTo ListChangeReason.Refresh
    }

    @Test
    fun autoRefreshBatched() {
        val scheduler = TestScheduler()
        val items = (1..100).map { Person("Person$it", 1) }.toList()
        val list = SourceList<Person>()
        val result = list.connect().autoRefresh(Person::age, 1, TimeUnit.SECONDS, scheduler = scheduler).asAggregator()

        list.addRange(items)
        result.data.size shouldBeEqualTo 100
        result.messages.size shouldBeEqualTo 1

        items.drop(50)
            .forEach { it.age = it.age + 1 }

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        result.messages.size shouldBeEqualTo 2
        result.messages[1].refreshes shouldBeEqualTo 50
    }

    @Test
    fun autoRefreshFilter() {
        val items = (1..100).map { Person("Person$it", it) }.toList()
        val list = SourceList<Person>()
        val result = list.connect()
            .autoRefresh(Person::age)
            .filterItem { it.age > 50 }
            .asAggregator()

        list.addRange(items)
        result.data.size shouldBeEqualTo 50
        result.messages.size shouldBeEqualTo 1

        items[0].age = 60
        result.data.size shouldBeEqualTo 51
        result.messages.size shouldBeEqualTo 2
        result.messages[1].first().reason shouldBeEqualTo ListChangeReason.Add

        items[0].age = 21
        result.data.size shouldBeEqualTo 50
        result.messages.last().first().reason shouldBeEqualTo ListChangeReason.Remove
        items[0].age = 60

        items[60].age = 160
        result.data.size shouldBeEqualTo 51
        result.messages.size shouldBeEqualTo 5
        result.messages.last().first().reason shouldBeEqualTo ListChangeReason.Replace

        val toRemove = items[65]
        list.remove(toRemove)
        result.data.size shouldBeEqualTo 50
        result.messages.size shouldBeEqualTo 6

        toRemove.age = 100
        result.messages.size shouldBeEqualTo 6

        list.add(toRemove)
        result.messages.size shouldBeEqualTo 7

        toRemove.age = 101
        result.messages.size shouldBeEqualTo 8
        result.messages.last().first().reason shouldBeEqualTo ListChangeReason.Replace
    }

    @Test
    fun autoRefreshTransform() {
        val items = (1..100).map { Person("Person$it", it) }.toList()
        val list = SourceList<Person>()
        val result = list.connect()
            .autoRefresh(Person::age)
            .transform { p, idx -> TransformedPerson(p, idx) }
            .asAggregator()

        list.addRange(items)
        result.data.size shouldBeEqualTo 100
        result.messages.size shouldBeEqualTo 1

        items[0].age = 60
        result.messages.size shouldBeEqualTo 2
        result.messages.last().refreshes shouldBeEqualTo 1
        result.messages.last().first().item.reason shouldBeEqualTo ListChangeReason.Refresh
        result.messages.last().first().item.current.index shouldBeEqualTo 0

        items[60].age = 160
        result.messages.size shouldBeEqualTo 3
        result.messages.last().refreshes shouldBeEqualTo 1
        result.messages.last().first().item.reason shouldBeEqualTo ListChangeReason.Refresh
        result.messages.last().first().item.current.index shouldBeEqualTo 60
    }

    data class TransformedPerson(
        val person: Person,
        val index: Int
    )
}
