package dynamicdata.list

import dynamicdata.list.test.ChangeSetAggregator
import dynamicdata.list.test.asAggregator
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

class XOrFixture : XOrFixtureBase() {
    override val result: ChangeSetAggregator<Int> =
        source1.connect().xor(source2.connect()).asAggregator()
}

class XOrCollectionFixture : XOrFixtureBase() {
    override val result: ChangeSetAggregator<Int> =
        listOf(source1.connect(), source2.connect()).xor().asAggregator()
}

abstract class XOrFixtureBase {
    val source1 = SourceList<Int>()
    val source2 = SourceList<Int>()
    abstract val result: ChangeSetAggregator<Int>

    @Test
    fun includedWhenItemIsInOneSource() {
        source1.add(1)

        result.data.size shouldBeEqualTo 1
        result.data.first() shouldBeEqualTo 1
    }

    @Test
    fun notIncludedWhenItemIsInTwoSources() {
        source1.add(1)
        source2.add(1)

        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun removedWhenNoLongerInBoth() {
        source1.add(1)
        source2.add(1)
        source1.remove(1)

        result.data.size shouldBeEqualTo 1
    }

    @Test
    fun removedWhenNoLongerInEither() {
        source1.add(1)
        source1.remove(1)

        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun combineRange() {
        source1.addRange(1..5)
        source2.addRange(6..10)

        result.data.size shouldBeEqualTo 10
        result.data.toList() shouldBeEqualTo (1..10).toList()
    }

    @Test
    fun clearOnlyClearsOneSource() {
        source1.addRange(1..5)
        source2.addRange(6..10)
        source1.clear()

        result.data.size shouldBeEqualTo 5
        result.data.toList() shouldBeEqualTo (6..10).toList()
    }

    @Test
    fun overlappingRangeExcludesIntersect() {
        source1.addRange(1..10)
        source2.addRange(6..15)

        result.data.size shouldBeEqualTo 10
        result.data.toList() shouldBeEqualTo (1..5).union(11..15).toList()
    }
}
