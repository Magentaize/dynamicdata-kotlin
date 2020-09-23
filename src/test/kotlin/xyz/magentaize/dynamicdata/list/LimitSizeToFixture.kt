package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.domain.Person
import xyz.magentaize.dynamicdata.domain.RandomPersonGenerator
import xyz.magentaize.dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.schedulers.TestScheduler
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldThrow
import java.util.concurrent.TimeUnit
import kotlin.test.Test

internal class LimitSizeToFixture {
    val source = SourceList<Person>()
    val scheduler = TestScheduler()
    val sizeLimiter = source.limitSizeTo(10, scheduler).subscribe()
    val result = source.connect().asAggregator()

    @Test
    fun addLessThanLimit() {
        val p = RandomPersonGenerator.take(1).first()
        source.add(p)
        scheduler.advanceTimeBy(150, TimeUnit.MILLISECONDS)

        result.messages.size shouldBeEqualTo 1
        result.data.size shouldBeEqualTo 1
        result.data.first() shouldBe p
    }

    @Test
    fun addMoreThanLimit() {
        val people = RandomPersonGenerator.take(100)
        source.addRange(people)
        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        source.dispose()

        result.data.size shouldBeEqualTo 10
        result.messages.size shouldBeEqualTo 2
        result.messages[0].adds shouldBeEqualTo 100
        result.messages[1].removes shouldBeEqualTo 90
    }

    @Test
    fun addMoreThanLimitInBatched() {
        source.addRange(RandomPersonGenerator.take(10))
        source.addRange(RandomPersonGenerator.take(10))
        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        result.data.size shouldBeEqualTo 10
        result.messages.size shouldBeEqualTo 3
        result.messages[0].adds shouldBeEqualTo 10
        result.messages[1].adds shouldBeEqualTo 10
        result.messages[2].removes shouldBeEqualTo 10
    }

    @Test
    fun add() {
        val p = RandomPersonGenerator.take(1).first()
        source.add(p)

        result.messages.size shouldBeEqualTo 1
        result.data.size shouldBeEqualTo 1
        result.data.first() shouldBe p
    }

    @Test
    fun throwsIfSizeLimitIsZero() {
        invoking { source.limitSizeTo(0) }
            .shouldThrow(IllegalArgumentException::class)
    }
}

