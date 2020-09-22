package dynamicdata.list

import dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.core.Observable
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class DynamicAndFixture{
    private val source1 = SourceList<Int>()
    private val source2 = SourceList<Int>()
    private val source3 = SourceList<Int>()
    private val source = SourceList<Observable<ChangeSet<Int>>>()
    private val result = source.and().asAggregator()

    @Test
    fun excludedWhenItemIsInOneSource(){
        source.add(source1.connect())
        source.add(source2.connect())
        source1.add(1)

        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun includedWhenItemIsInTwoSources(){
        source.add(source1.connect())
        source.add(source2.connect())
        source1.add(1)
        source2.add(1)

        result.data.size shouldBeEqualTo 1
    }

    @Test
    fun removedWhenNoLongerInBoth(){
        source.add(source1.connect())
        source.add(source2.connect())
        source1.add(1)
        source2.add(1)
        source1.remove(1)

        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun combineRange(){
        source.add(source1.connect())
        source.add(source2.connect())
        source1.addRange(1..10)
        source2.addRange(6..10)

        result.data.size shouldBeEqualTo 5
        result.data.items.toList() shouldBeEqualTo (6..10).toList()
    }

    @Test
    fun clearOneClearsResult(){
        source.add(source1.connect())
        source.add(source2.connect())
        source1.addRange(1..5)
        source2.addRange(1..5)
        source1.clear()

        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun addAndRemoveLists(){
        source1.addRange(1..5)
        source3.addRange(1..5)

        source.add(source1.connect())
        source.add(source3.connect())

        val ex = (1..5).toList()

        result.data.size shouldBeEqualTo 5
        result.data.items.toList() shouldBeEqualTo ex

        source2.addRange(6..10)
        result.data.size shouldBeEqualTo 5

        source.add(source2.connect())
        result.data.size shouldBeEqualTo 0

        source.removeAt(2)
        result.data.size shouldBeEqualTo 5
        result.data.items.toList() shouldBeEqualTo ex
    }
}
