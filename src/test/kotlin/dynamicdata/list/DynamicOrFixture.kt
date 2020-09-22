package dynamicdata.list

import dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.core.Observable
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class DynamicOrFixture {
    private val source1 = SourceList<Int>()
    private val source2 = SourceList<Int>()
    private val source3 = SourceList<Int>()
    private val source = SourceList<Observable<ChangeSet<Int>>>()
    private val result = source.or().asAggregator()

    @Test
    fun itemIsReplaced() {
        source1.add(0)
        source2.add(1)
        source.add(source1.connect())
        source.add(source2.connect())
        source1.replaceAt(0, 9)

        result.data.size shouldBeEqualTo 2
        result.messages.size shouldBeEqualTo 3
        result.data.items.toList() shouldBeEqualTo listOf(1, 9)
    }

    @Test
    fun clearSource() {
        source1.add(0)
        source2.add(1)
        source.add(source1.connect())
        source.add(source2.connect())
        source.clear()

        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun includedWhenItemIsInOneSource() {
        source.add(source1.connect())
        source.add(source2.connect())
        source1.add(1)

        result.data.size shouldBeEqualTo 1
        result.data.items.first() shouldBeEqualTo 1
    }

    @Test
    fun includedWhenItemIsInTwoSources() {
        source.add(source1.connect())
        source.add(source2.connect())
        source1.add(1)
        source2.add(1)

        result.data.size shouldBeEqualTo 1
        result.data.items.first() shouldBeEqualTo 1
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
        source1.addRange(1..10)
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
    fun addAndRemoveLists() {
        source1.addRange(1..5)
        source2.addRange(6..10)
        source3.addRange(100..104)

        source.add(source1.connect())
        source.add(source2.connect())
        source.add(source3.connect())

        var ex = (1..5).union(6..10).union(100..104).toList()

        result.data.size shouldBeEqualTo 15
        result.data.toList() shouldBeEqualTo ex

        source.removeAt(1)
        result.data.size shouldBeEqualTo 10

        ex = (1..5).union(100..104).toList()
        result.data.items.toList() shouldBeEqualTo ex
    }
//
//    @Test
//    fun refreshPassesThrough(){
//        source1.add()
//    }
}
