package dynamicdata.list

import dynamicdata.domain.Person
import dynamicdata.list.test.asAggregator
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
        .bufferIf
        .buffer(1, TimeUnit.MINUTES, scheduler)
        .flattenBufferResult()
        .asAggregator()

    @Test
    fun noResultsWillBeReceivedBeforeClosingBuffer(){
        source.add(Person("A", 1))
        results.messages.size shouldBeEqualTo 0
    }

    @Test
    fun resultsWillBeReceivedAfterClosingBuffer(){
        source.add(Person("A", 1))

        //go forward an arbitrary amount of time
        scheduler.advanceTimeBy(61, TimeUnit.SECONDS)
        results.messages.size shouldBeEqualTo 1
    }
}
