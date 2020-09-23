package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.domain.Person
import xyz.magentaize.dynamicdata.list.test.asAggregator
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class DistinctValuesFixture {
    private val source = SourceList<Person>()
    private val result = source.connect().distinctValues { it.age }.asAggregator()

    @Test
    fun firesAddWhenNewItemIsAdded() {
        source.add(Person("Person1", 20))

        result.messages.size shouldBeEqualTo 1
        result.data.size shouldBeEqualTo 1
        result.data.items.first() shouldBeEqualTo 20
    }

    @Test
    fun firesBatchResultOnce() {
        source.edit {
            it.add(Person("Person1", 20))
            it.add(Person("Person2", 21))
            it.add(Person("Person3", 22))
        }

        result.messages.size shouldBeEqualTo 1
        result.data.size shouldBeEqualTo 3
        result.data.items.toList() shouldBeEqualTo listOf(20, 21, 22)
        result.data.items.first() shouldBeEqualTo 20
    }

    @Test
    fun duplicatedResultsResultInNoAdditionalMessage() {
        source.edit {
            it.add(Person("Person1", 20))
            it.add(Person("Person1", 20))
            it.add(Person("Person1", 20))
        }

        result.messages.size shouldBeEqualTo 1
        result.data.size shouldBeEqualTo 1
        result.data.items.first() shouldBeEqualTo 20
    }

    @Test
    fun removingAnItemRemovesTheDistinct() {
        val person = Person("Person1", 20)

        source.add(person)
        source.remove(person)

        result.messages.size shouldBeEqualTo 2
        result.data.size shouldBeEqualTo 0
        result.messages.first().adds shouldBeEqualTo 1
        result.messages.drop(1).first().removes shouldBeEqualTo 1
    }

    @Test
    fun replacing() {
        val person = Person("A", 20)
        val replaceWith = Person("A", 21)

        source.add(person)
        source.replace(person, replaceWith)

        result.messages.size shouldBeEqualTo 2
        result.data.size shouldBeEqualTo 1
        result.messages.first().adds shouldBeEqualTo 1
        result.messages.drop(1).first().size shouldBeEqualTo 2
    }

    @Test
    fun addingRemovedItem() {
        val person = Person("A", 20)

        source.add(person)
        source.remove(person)
        source.add(person)

        result.messages.size shouldBeEqualTo 3
        result.data.size shouldBeEqualTo 1
        result.data.items.toList() shouldBeEqualTo listOf(20)
        result.messages.elementAt(0).adds shouldBeEqualTo 1
        result.messages.elementAt(1).removes shouldBeEqualTo 1
        result.messages.elementAt(2).adds shouldBeEqualTo 1
    }
}
