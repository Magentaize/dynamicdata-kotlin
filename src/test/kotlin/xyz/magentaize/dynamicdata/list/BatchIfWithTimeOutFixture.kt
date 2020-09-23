package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.domain.Person
import xyz.magentaize.dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import org.amshove.kluent.shouldBeEqualTo
import java.util.concurrent.TimeUnit
import kotlin.test.Test

internal class BatchIfWithTimeOutFixture {
    private val scheduler = TestScheduler()
    private val pausingSubject = PublishSubject.create<Boolean>()
    private val source = SourceList<Person>()
    private val results = source.connect()
        .bufferIf(pausingSubject, 1, TimeUnit.MINUTES, scheduler)
        .asAggregator()

    @Test
    fun willApplyTimeout() {
        pausingSubject.onNext(true)
        scheduler.advanceTimeBy(61, TimeUnit.MINUTES)
        source.add(Person("A", 1))

        results.messages.size shouldBeEqualTo 1
    }

    @Test
    fun noResultsWillBeReceivedIfPaused() {
        pausingSubject.onNext(true)
        scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)
        source.add(Person("A", 1))

        results.messages.size shouldBeEqualTo 0
    }

    @Test
    fun resultsWillBeReceivedIfNotPaused() {
        source.add(Person("A", 1))
        scheduler.advanceTimeBy(1, TimeUnit.MINUTES)

        results.messages.size shouldBeEqualTo 1
    }

    @Test
    fun canToggleSuspendResume() {
        pausingSubject.onNext(true)
        scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)
        source.add(Person("A", 1))

        results.messages.size shouldBeEqualTo 0

        pausingSubject.onNext(false)
        scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)
        source.add(Person("B", 1))

        results.messages.size shouldBeEqualTo 2
    }
}
