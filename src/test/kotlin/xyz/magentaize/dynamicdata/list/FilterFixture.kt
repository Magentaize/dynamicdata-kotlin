package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.domain.Person
import xyz.magentaize.dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.subjects.Subject
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class FilterFixture : FilterBaseFixture() {
    override val filter: Subject<(Person) -> Boolean>
        get() = throw Exception("Not implemented")
    override val result = source.connect { it.age > 20 }.asAggregator()

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
    fun addSubscribeRemove() {
        val people = (1..100).map { Person("Name$it", it) }.toList()
        val source = SourceList<Person>()
        source.addRange(people)
        val result = source.connect { it.age > 20 }.asAggregator()
        source.removeAll(people.filter { it.age % 2 == 0 })

        result.data.size shouldBeEqualTo 40
    }
}
