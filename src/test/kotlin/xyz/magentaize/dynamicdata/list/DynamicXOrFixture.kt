package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.core.Observable
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class DynamicXOrFixture {
    private val source1 = SourceList<Int>()
    private val source2 = SourceList<Int>()
    private val source3 = SourceList<Int>()
    private val source = SourceList<Observable<ChangeSet<Int>>>()
    private val result = source.xor().asAggregator()

    @Test
    fun includedWhenItemIsInOneSource() {
        source.add(source1.connect())
        source.add(source2.connect())
        source1.add(1)

        result.data.size shouldBeEqualTo 1
        result.data.items.first() shouldBeEqualTo 1
    }

    @Test
    fun notIncludedWhenItemIsInTwoSources() {
        source.add(source1.connect())
        source.add(source2.connect())
        source1.add(1)
        source2.add(1)

        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun removedWhenNoLongerInBoth() {
        source.add(source1.connect())
        source.add(source2.connect())
        source1.add(1)
        source2.add(1)
        source1.remove(1)

        result.data.size shouldBeEqualTo 1
    }

    @Test
    fun removedWhenNoLongerInEither() {
        source.add(source1.connect())
        source.add(source2.connect())
        source1.add(1)
        source1.remove(1)

        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun combineRange() {
        source.add(source1.connect())
        source.add(source2.connect())
        source1.addRange(1..5)
        source2.addRange(6..10)

        result.data.size shouldBeEqualTo 10
        result.data.items.toList() shouldBeEqualTo (1..10).toList()
    }

    @Test
    fun clearOnlyClearsOneSource() {
        source.add(source1.connect())
        source.add(source2.connect())
        source1.addRange(1..5)
        source2.addRange(6..10)
        source1.clear()

        result.data.size shouldBeEqualTo 5
        result.data.items.toList() shouldBeEqualTo (6..10).toList()
    }

    @Test
    fun overlappingRangeExcludesIntersect() {
        source.add(source1.connect())
        source.add(source2.connect())
        source1.addRange(1..10)
        source2.addRange(6..15)

        result.data.size shouldBeEqualTo 10
        result.data.items.toList() shouldBeEqualTo (1..5).union(11..15).toList()
    }

    @Test
    fun addAndRemoveLists() {
        source1.addRange(1..5)
        source2.addRange(6..10)
        source3.addRange(1..5)

        source.add(source1.connect())
        source.add(source2.connect())
        source.add(source3.connect())

        var ex = (6..10).toList()

        result.data.size shouldBeEqualTo 5
        result.data.toList() shouldBeEqualTo ex

        source.removeAt(0)
        ex = (6..10).union(1..5).toList()
        result.data.size shouldBeEqualTo 10
        result.data.items.toList() shouldBeEqualTo ex

        source.add(source1.connect())
        ex = (6..10).toList()
        result.data.size shouldBeEqualTo 5
        result.data.toList() shouldBeEqualTo ex
    }
}
