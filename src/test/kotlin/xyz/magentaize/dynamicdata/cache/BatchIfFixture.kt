package xyz.magentaize.dynamicdata.cache

import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import org.amshove.kluent.shouldBeEqualTo
import xyz.magentaize.dynamicdata.domain.Person
import xyz.magentaize.dynamicdata.kernel.advanceTimeBy
import kotlin.test.Test
import kotlin.time.milliseconds
import kotlin.time.minutes

class BatchIfFixture {
    private val scheduler = TestScheduler()
    private val pausingSubject = PublishSubject.create<Boolean>()
    private val source = SourceCache<String, Person> { it.name }
    private val result = source.connect().batchIf(pausingSubject, scheduler).asAggregator()

    @Test
    fun canToggleSuspendResume() {
        pausingSubject.onNext(true)
        scheduler.advanceTimeBy(10.milliseconds)
        source.addOrUpdate(Person("A", 1))
        scheduler.advanceTimeBy(1.minutes)
        result.messages.size shouldBeEqualTo 0

        pausingSubject.onNext(false)
        scheduler.advanceTimeBy(10.milliseconds)
        source.addOrUpdate(Person("B", 1))
        result.messages.size shouldBeEqualTo 2

        pausingSubject.onNext(true)
        scheduler.advanceTimeBy(10.milliseconds)
        source.addOrUpdate(Person("C", 1))
        scheduler.advanceTimeBy(1.minutes)
        result.messages.size shouldBeEqualTo 2

        pausingSubject.onNext(false)
        scheduler.advanceTimeBy(10.milliseconds)
        result.messages.size shouldBeEqualTo 3
    }

    @Test
    fun changesNotLostIfConsumerIsRunningOnDifferentThread(){
        val producerScheduler = TestScheduler()
        val consumerScheduler = TestScheduler()

        source.connect().batchIf(pausingSubject, producerScheduler).observeOn(consumerScheduler)
    }

    @Test
    fun noResultsWillBeReceivedIfPaused(){
        pausingSubject.onNext(true)
        scheduler.advanceTimeBy(10.milliseconds)
        source.addOrUpdate(Person("A",1))
        scheduler.advanceTimeBy(1.minutes)
        result.messages.size shouldBeEqualTo 0
    }

    @Test
    fun resultsWillBeReceivedIfNotPaused(){
        source.addOrUpdate(Person("A",1))
        scheduler.advanceTimeBy(1.minutes)
        result.messages.size shouldBeEqualTo 1
    }
}