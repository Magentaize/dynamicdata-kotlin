package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.domain.Person
import xyz.magentaize.dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.schedulers.TestScheduler
import org.amshove.kluent.shouldBeEqualTo
import java.util.concurrent.TimeUnit
import kotlin.test.Test

internal class BufferInitialFixture {
    private val people = (1..10_000).map { Person(it.toString(), it) }.toList()

    @Test
    fun bufferInitial() {
        val scheduler = TestScheduler()
        val cache = SourceList<Person>()
        val aggregator = cache.connect()
            .bufferInitial(1, TimeUnit.SECONDS, scheduler)
            .asAggregator()
        people.forEach { cache.add(it) }

        aggregator.data.size shouldBeEqualTo 0
        aggregator.messages.size shouldBeEqualTo 0

        scheduler.start()
        // TestScheduler not implements start(), call advanceBy manually
        scheduler.advanceTimeBy(61, TimeUnit.SECONDS)

        aggregator.data.size shouldBeEqualTo 10_000
        aggregator.messages.size shouldBeEqualTo 1

        cache.add(Person("_New", 1))

        aggregator.data.size shouldBeEqualTo 10_001
        aggregator.messages.size shouldBeEqualTo 2
    }
}
