package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.domain.Person
import xyz.magentaize.dynamicdata.domain.RandomPersonGenerator
import xyz.magentaize.dynamicdata.list.test.asAggregator
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class GroupOnMutableFixture {
    val source = SourceList<Person>()
    val result = source.connect()
        .groupOnMutable { it.age }
        .asAggregator()

    @Test
    fun add() {
        val p = Person("P", 50)
        source.add(p)

        result.messages.size shouldBeEqualTo 1
        result.data.size shouldBeEqualTo 1

        result.data.items.first().list.toList()[0] shouldBeEqualTo p
    }

    @Test
    fun remove() {
        val p = Person("P", 50)
        source.add(p)
        source.remove(p)

        result.messages.size shouldBeEqualTo 2
        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun updateWillChangeTheGroup() {
        val person = Person("P", 50)
        val amended = Person("P", 60)
        source.add(person)
        source.replaceAt(0, amended)

        result.messages.size shouldBeEqualTo 2
        result.data.size shouldBeEqualTo 1
        result.data.items.first().list.toList()[0] shouldBeEqualTo amended
    }

    @Test
    fun bigList() {
        val people = RandomPersonGenerator.take(10000).toList()
        source.addRange(people)
    }
}
