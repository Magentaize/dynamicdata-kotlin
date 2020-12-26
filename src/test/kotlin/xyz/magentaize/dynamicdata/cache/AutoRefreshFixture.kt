package xyz.magentaize.dynamicdata.cache

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import xyz.magentaize.dynamicdata.domain.Person
import kotlin.test.Test

class AutoRefreshFixture {
    @Test
    fun autoRefresh() {
        val items = Person.make100People()
        val cache = SourceCache<String, Person> { it.name }
        val result = cache.connect().autoRefresh(Person::age).asAggregator()

        cache.addOrUpdate(items)

        result.data.size shouldBeEqualTo 100
        result.messages.size shouldBeEqualTo 1

        items[0].age = 10
        result.data.size shouldBeEqualTo 100
        result.messages.size shouldBeEqualTo 2
        result.messages[1].first().reason shouldBe ChangeReason.Refresh

        val remove = items[1]
        cache.removeItem(remove)
        result.data.size shouldBeEqualTo 99
        result.messages.size shouldBeEqualTo 3
        remove.age = 100
        result.messages.size shouldBeEqualTo 3

        cache.addOrUpdate(remove)
        result.messages.size shouldBeEqualTo 4
        remove.age = 101
        result.messages.size shouldBeEqualTo 5
        result.messages.last().first().reason shouldBe ChangeReason.Refresh
    }
}