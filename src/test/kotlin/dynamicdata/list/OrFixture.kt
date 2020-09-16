package dynamicdata.list

import dynamicdata.domain.Item
import dynamicdata.list.test.ChangeSetAggregator
import dynamicdata.list.test.asAggregator
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class OrRefreshFixture {
    @Test
    fun refreshPassesThrough() {
        val source1 = SourceList<Item>()
        source1.add(Item("A"))
        val source2 = SourceList<Item>()
        source2.add(Item("B"))

        val result = listOf(source1.connect().autoRefresh(), source2.connect().autoRefresh()).or().asAggregator()
        source1.items.elementAt(0).name = "Test"

        result.data.size shouldBeEqualTo 2
        result.messages.size shouldBeEqualTo 3
        result.messages[2].refreshes shouldBeEqualTo 1
        result.messages[2].first().item.current shouldBe source1.items.first()
    }
}

internal class OrFixture : OrFixtureBase() {
    override val result: ChangeSetAggregator<Int> =
        source1.connect().or(source2.connect()).asAggregator()
}

internal class OrCollectionFixture : OrFixtureBase() {
    override val result: ChangeSetAggregator<Int> =
        listOf(source1.connect(), source2.connect())
            .or().asAggregator()
}

internal abstract class OrFixtureBase {
    val source1 = SourceList<Int>()
    val source2 = SourceList<Int>()
    abstract val result: ChangeSetAggregator<Int>

    @Test
    fun includedWhenItemIsInOneSource() {
        source1.add(1)

        result.data.size shouldBeEqualTo 1
        result.data.items.first() shouldBeEqualTo 1
    }

    @Test
    fun includedWhenItemIsInTwoSources() {
        source1.add(1)
        source2.add(1)

        result.data.size shouldBeEqualTo 1
        result.data.items.first() shouldBeEqualTo 1
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
        result.data.items.toList() shouldBeEqualTo (1..10).toList()
    }

    @Test
    fun clearOnlyClearsOneSource() {
        source1.addRange(1..5)
        source2.addRange(6..10)
        source1.clear()

        result.data.size shouldBeEqualTo 5
        result.data.items.toList() shouldBeEqualTo (6..10).toList()
    }
}
