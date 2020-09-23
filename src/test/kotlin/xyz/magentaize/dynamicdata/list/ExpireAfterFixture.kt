package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.domain.Person
import xyz.magentaize.dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.schedulers.TestScheduler
import org.amshove.kluent.shouldBeEqualTo
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

@ExperimentalTime
internal class ExpireAfterFixture {
    private val scheduler = TestScheduler()
    private val source = SourceList<Person>()
    private val result = source.connect().asAggregator()

    @Test
    fun complexRemove() {
        fun removeFunc(t: Person): Duration? {
            if (t.age <= 40)
                return 5.toDuration(TimeUnit.SECONDS)

            if (t.age <= 80)
                return 7.toDuration(TimeUnit.SECONDS)

            return null
        }

        val size = 100
        val items = (1..size).map { Person("Name.{$it}", it) }.toList()
        source.addRange(items)

        val remover = source.expireAfter(::removeFunc, scheduler).subscribe()

        scheduler.advanceTimeBy(5010, TimeUnit.MILLISECONDS)
        source.size shouldBeEqualTo 60

        scheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        source.size.shouldBeEqualTo(20)
    }

    @Test
    fun itemAddedIsExpired() {
        val remover = source.expireAfter({ 100.toDuration(TimeUnit.MILLISECONDS) }, scheduler).subscribe()
        source.add(Person("Name1", 10))

        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS)
        remover.dispose()

        result.messages.size shouldBeEqualTo 2
        result.messages[0].adds shouldBeEqualTo 1
        result.messages[1].removes shouldBeEqualTo 1
    }

    @Test
    fun expireIsCancelledWhenUpdated() {
        val remover = source.expireAfter({ 100.toDuration(TimeUnit.MILLISECONDS) }, scheduler).subscribe()
        val p1 = Person("Name1", 20)
        val p2 = Person("Name2", 21)
        source.add(p1)
        source.replace(p1, p2)

        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS)
        remover.dispose()

        result.data.size shouldBeEqualTo 0
        result.messages.size shouldBeEqualTo 3
        result.messages[0].adds shouldBeEqualTo 1
        result.messages[1].replaced shouldBeEqualTo 1
        result.messages[2].removes shouldBeEqualTo 1
    }

    @Test
    fun canHandleABatchOfUpdates() {
        val remover = source.expireAfter({ 100.toDuration(TimeUnit.MILLISECONDS) }, scheduler).subscribe()
        val size = 100
        val items = (1..size).map { Person("Name.{$it}", it) }.toList()
        source.addRange(items)

        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS)
        remover.dispose()

        result.data.size shouldBeEqualTo 0
        result.messages.size shouldBeEqualTo 2
        result.messages[0].adds shouldBeEqualTo 100
        result.messages[1].removes shouldBeEqualTo 100
    }
}
