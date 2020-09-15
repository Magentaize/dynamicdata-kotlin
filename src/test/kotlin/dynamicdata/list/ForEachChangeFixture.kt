package dynamicdata.list

import dynamicdata.domain.Person
import dynamicdata.domain.RandomPersonGenerator
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class ForEachChangeFixture {
    val source = SourceList<Person>()

    @Test
    fun eachChangeInvokesTheCallback() {
        val msg = mutableListOf<Change<Person>>()
        val msgWriter = source.connect()
            .forEachChange(msg::add)
            .subscribe()
        val people = RandomPersonGenerator.take(100)
        people.forEach(source::add)

        msg.size shouldBeEqualTo 100
    }

    @Test
    fun eachItemChangeInvokesTheCallback() {
        val msg = mutableListOf<ItemChange<Person>>()
        val msgWriter = source.connect()
            .forEachItemChange(msg::add)
            .subscribe()
        val people = RandomPersonGenerator.take(100)
        source.addRange(people)

        msg.size shouldBeEqualTo 100
    }

    @Test
    fun eachItemChangeInvokesTheCallback2() {
        val msg = mutableListOf<ItemChange<Person>>()
        val msgWriter = source.connect()
            .forEachItemChange(msg::add)
            .subscribe()
        source.addRange(RandomPersonGenerator.take(5))
        source.addRange(RandomPersonGenerator.take(5), 2)
        source.addRange(RandomPersonGenerator.take(5))

        msg.size shouldBeEqualTo 15
    }
}
