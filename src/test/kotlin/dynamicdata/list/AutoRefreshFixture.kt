package dynamicdata.list

import dynamicdata.domain.Person
import dynamicdata.list.test.asAggregator
import org.amshove.kluent.shouldBeEqualTo
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
}
