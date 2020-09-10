package dynamicdata.list

import dynamicdata.domain.Person
import dynamicdata.list.test.asAggregator
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class EditDiffFixture {
    private val cache = SourceList<Person>().apply { addRange((1..10).map { Person("Name$it", it) }) }
    private val result = cache.connect().asAggregator()
    private val nameAgeGenderComparer =
        { o1: Person, o2: Person -> (o1.name == o2.name && o1.age == o2.age && o1.gender == o2.gender) }

    @Test
    fun new() {
        val new = (1..15).map { Person("Name$it", it) }
        cache.editDiff(new, nameAgeGenderComparer)

        cache.size shouldBeEqualTo 15
        cache.items.toList() shouldBeEqualTo new
        result.messages.last().adds shouldBeEqualTo 5
    }

    @Test
    fun editWithSameData() {
        val new = (1..10).map { Person("Name$it", it) }
        cache.editDiff(new, nameAgeGenderComparer)

        cache.size shouldBeEqualTo 10
        cache.items.toList() shouldBeEqualTo new
        result.messages.size shouldBeEqualTo 1
    }

    @Test
    fun amends() {
        val new = (5..7).map { Person("Name$it", it + 10) }
        cache.editDiff(new, nameAgeGenderComparer)

        cache.size shouldBeEqualTo 3
        result.messages.last().adds shouldBeEqualTo 3
        result.messages.last().removes shouldBeEqualTo 10
        cache.items.toList() shouldBeEqualTo new
    }

    @Test
    fun removes() {
        val new = (1..7).map { Person("Name$it", it) }
        cache.editDiff(new, nameAgeGenderComparer)

        cache.size shouldBeEqualTo 7
        result.messages.last().adds shouldBeEqualTo 0
        result.messages.last().removes shouldBeEqualTo 3
        cache.items.toList() shouldBeEqualTo new
    }

    @Test
    fun variousChanges() {
        val new = (6..15).map { Person("Name$it", it) }
        cache.editDiff(new, nameAgeGenderComparer)

        cache.size shouldBeEqualTo 10
        result.messages.last().adds shouldBeEqualTo 5
        result.messages.last().removes shouldBeEqualTo 5
        cache.items.toList() shouldBeEqualTo new
    }
}
