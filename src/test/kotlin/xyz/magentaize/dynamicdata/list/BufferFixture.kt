package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.domain.Person
import xyz.magentaize.dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.schedulers.TestScheduler
import org.amshove.kluent.shouldBeEqualTo
import java.util.concurrent.TimeUnit
import kotlin.test.Test

internal class BufferFixture {
    private val scheduler = TestScheduler()
    private val source = SourceList<Person>()
    private val results = source.connect()
        .buffer(1, TimeUnit.MINUTES, scheduler)
        .flattenBufferResult()
        .asAggregator()

    @Test
    fun noResultsWillBeReceivedBeforeClosingBuffer() {
        source.add(Person("A", 1))

        results.messages.size shouldBeEqualTo 0
    }

    @Test
    fun resultsWillBeReceivedAfterClosingBuffer() {
        source.add(Person("A", 1))
        scheduler.advanceTimeBy(61, TimeUnit.MINUTES)

        results.messages.size shouldBeEqualTo 1
    }
}
