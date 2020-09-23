package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.list.test.asAggregator
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

class ReverseFixture {
    val source = SourceList<Int>()
    val result = source.connect().reverse().asAggregator()

    @Test
    fun addRange() {
        source.addRange(1..5)
        result.data.toList() shouldBeEqualTo (5 downTo 1).toList()
    }

    @Test
    fun removes() {
        source.addRange(1..5)
        source.remove(1)
        source.remove(4)
        result.data.toList() shouldBeEqualTo listOf(5, 3, 2)
    }

    @Test
    fun removeRange() {
        source.addRange(1..5)
        source.removeRange(1, 3)
        result.data.toList() shouldBeEqualTo listOf(5, 1)
    }

    @Test
    fun removeRangeThenInsert() {
        source.addRange(1..5)
        source.removeRange(1, 3)
        source.add(1, 3)
        result.data.toList() shouldBeEqualTo listOf(5, 3, 1)
    }

    @Test
    fun replace() {
        source.addRange(1..5)
        source.replaceAt(2, 100)
        result.data.toList() shouldBeEqualTo listOf(5, 4, 100, 2, 1)
    }

    @Test
    fun clear() {
        source.addRange(1..5)
        source.clear()
        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun move() {
        source.addRange(1..5)
        source.move(4, 1)
        result.data.toList() shouldBeEqualTo listOf(4, 3, 2, 5, 1)
    }

    @Test
    fun move2() {
        source.addRange(1..5)
        source.move(1, 4)
        result.data.toList() shouldBeEqualTo listOf(2, 5, 4, 3, 1)
    }
}
