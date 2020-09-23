package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.domain.Person
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class DeferUntilLoadedFixture{
    @Test
    fun deferUntilLoadedDoesNothingUntilDataHasBeenReceived(){
        var updateReceived = false
        var result: ChangeSet<Person>? = null
        val cache = SourceList<Person>()

        val deferStream = cache.connect()
            .deferUntilLoaded()
            .subscribe{
                updateReceived = true
                result = it
            }

        val person = Person("Test", 1)

        updateReceived shouldBeEqualTo false

        cache.add(person)

        updateReceived shouldBeEqualTo true
        result!!.adds shouldBeEqualTo 1
        result!!.unified().first().current shouldBeEqualTo person

        deferStream.dispose()
    }

    @Test
    fun skipInitialDoesNotReturnTheFirstBatchOfData(){
        var updateReceived = false
        val cache = SourceList<Person>()

        val deferStream = cache.connect()
            .skipInitial()
            .subscribe { updateReceived = true }

        updateReceived shouldBeEqualTo false

        cache.add(Person("P1", 1))

        updateReceived shouldBeEqualTo false

        cache.add(Person("P2", 1))

        updateReceived shouldBeEqualTo true

        deferStream.dispose()
    }
}
