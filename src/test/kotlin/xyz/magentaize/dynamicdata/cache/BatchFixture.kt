package xyz.magentaize.dynamicdata.cache

import io.reactivex.rxjava3.schedulers.TestScheduler
import org.amshove.kluent.shouldBeEqualTo
import xyz.magentaize.dynamicdata.domain.Person
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.time.minutes

class BatchFixture {
    private val scheduler = TestScheduler()
    private val source = SourceCache<String, Person> { it.name }
    private val result = source.connect().batch(1.minutes, scheduler).asAggregator()

    @Test
    fun noResultsWillBeReceivedBeforeClosingBuffer() {
        source.addOrUpdate(Person("A", 1))
        result.messages.size shouldBeEqualTo 0
    }

    @Test
    fun resultsWillBeReceivedAfterClosingBuffer() {
        source.addOrUpdate(Person("A", 1))
        scheduler.advanceTimeBy(61, TimeUnit.MINUTES)
        result.messages.size shouldBeEqualTo 1
    }
}
