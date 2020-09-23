package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.domain.Person
import xyz.magentaize.dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import org.amshove.kluent.shouldBeEqualTo
import java.util.concurrent.TimeUnit
import kotlin.test.Test

internal class BatchIfFixture {
    private val scheduler = TestScheduler()
    private val pausingSubject = PublishSubject.create<Boolean>()
    private val source = SourceList<Person>()
    private val results = source.connect()
        .bufferIf(pausingSubject, scheduler)
        .asAggregator()

    @Test
    fun changesNotLostIfConsumerIsRunningOnDifferentThread(){
        val producerScheduler = TestScheduler()
        val consumerScheduler = TestScheduler()

        //Note consumer is running on a different scheduler
        source.connect()
            .bufferIf(pausingSubject, producerScheduler)
            .observeOn(consumerScheduler)
            .asAggregator()
    }

    @Test
    fun resultsWillBeReceivedAfterClosingBuffer(){
        source.add(Person("A", 1))

        //go forward an arbitrary amount of time
        scheduler.advanceTimeBy(61, TimeUnit.SECONDS)
        results.messages.size shouldBeEqualTo 1
    }

    @Test
    fun noResultsWillBeReceivedIfPaused(){
        pausingSubject.onNext(true)
        scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)
        source.add(Person("A", 1))
        scheduler.advanceTimeBy(1, TimeUnit.MINUTES)

        results.messages.size shouldBeEqualTo 0
    }

    @Test
    fun resultsWillBeReceivedIfNotPaused(){
        source.add(Person("A", 1))
        scheduler.advanceTimeBy(1, TimeUnit.MINUTES)

        results.messages.size shouldBeEqualTo 1
    }

    @Test
    fun canToggleSuspendResume(){
        pausingSubject.onNext(true)
        scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)
        source.add(Person("A", 1))
        scheduler.advanceTimeBy(1, TimeUnit.MINUTES)

        results.messages.size shouldBeEqualTo 0

        pausingSubject.onNext(false)
        scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)
        source.add(Person("B", 1))

        results.messages.size shouldBeEqualTo 2
    }
}
