package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.domain.Person
import xyz.magentaize.dynamicdata.list.test.asAggregator
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class QueryWhenChangedFixture {
    val source = SourceList<Person>()
    val result = source.connect { it.age > 20 }.asAggregator()

    @Test
    fun changeInvokedOnSubscriptionIfItHasData() {
        var invoked = false
        source.add(Person("A", 1))
        source.connect()
            .queryWhenChanged()
            .subscribe { invoked = true }

        invoked shouldBeEqualTo true
    }

    @Test
    fun canHandleAddsAndUpdates() {
        var invoked = false
        source.connect()
            .queryWhenChanged()
            .subscribe { invoked = true }
        val p = Person("A", 1)
        source.add(p)
        source.remove(p)

        invoked shouldBeEqualTo true
    }

    @Test
    fun changeInvokedOnNext() {
        var invoked = false
        source.connect()
            .queryWhenChanged()
            .subscribe { invoked = true }
        invoked shouldBeEqualTo false

        val p = Person("A", 1)
        source.add(p)

        invoked shouldBeEqualTo true
    }

    @Test
    fun changeInvokedOnSubscriptionIfItHasData_WithSelector() {
        var invoked = false
        val p = Person("A", 1)
        source.add(p)
        source.connect()
            .queryWhenChanged { it.size }
            .subscribe { invoked = true }

        invoked shouldBeEqualTo true
    }

    @Test
    fun changeInvokedOnNext_WithSelector() {
        var invoked = false
        source.connect()
            .queryWhenChanged { it.size }
            .subscribe { invoked = true }
        invoked shouldBeEqualTo false

        val p = Person("A", 1)
        source.add(p)
        invoked shouldBeEqualTo true
    }
}
