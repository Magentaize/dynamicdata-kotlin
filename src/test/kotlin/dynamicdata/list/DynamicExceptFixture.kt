package dynamicdata.list

import dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.core.Observable
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class DynamicExceptFixture {
    private val source1 = SourceList<Int>()
    private val source2 = SourceList<Int>()
    private val source3 = SourceList<Int>()
    private val source = SourceList<Observable<ChangeSet<Int>>>()
    private val result = source.except().asAggregator()

    @Test
    fun includedWhenItemIsInOneSource(){
        source.add(source1.connect())
        source.add(source2.connect())
        source1.add(1)

        result.data.size shouldBeEqualTo 1
    }

    @Test
    fun nothingFromOther(){
        source.add(source1.connect())
        source.add(source2.connect())
        source2.add(1)

        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun includedWhenItemIsInTwoSources(){
        source.add(source1.connect())
        source.add(source2.connect())
        source1.add(1)
        source2.add(1)

        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun addedWhenNoLongerInSecond(){
        source.add(source1.connect())
        source.add(source2.connect())
        source1.add(1)
        source2.add(1)
        source2.remove(1)

        result.data.size shouldBeEqualTo 1
    }

    @Test
    fun combineRange(){
        source.add(source1.connect())
        source.add(source2.connect())
        source1.addRange(1..10)
        source2.addRange(6..10)

        result.data.size shouldBeEqualTo 5
        result.data.items.toList() shouldBeEqualTo (1..5).toList()
    }

    @Test
    fun clearFirstClearsResult(){
        source.add(source1.connect())
        source.add(source2.connect())
        source1.addRange(1..5)
        source2.addRange(1..5)
        source1.clear()

        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun clearSecondEnsuresFirstIsIncluded(){
        source.add(source1.connect())
        source.add(source2.connect())

        source1.addRange(1..5)
        source2.addRange(1..5)
        result.data.size shouldBeEqualTo 0

        source2.clear()
        result.data.size shouldBeEqualTo 5
        result.data.toList() shouldBeEqualTo (1..5).toList()
    }

    @Test
    fun addAndRemoveLists() {
        source1.addRange(1..5)
        source2.addRange(6..10)
        source3.addRange(100..104)

        source.add(source1.connect())
        source.add(source2.connect())
        source.add(source3.connect())

        var ex = (1..5).toList()

        result.data.size shouldBeEqualTo 5
        result.data.toList() shouldBeEqualTo ex

        source2.edit {
            it.clear()
            it.addAll(3..7)
        }
        ex = listOf(1, 2)
        result.data.size shouldBeEqualTo 2
        result.data.toList() shouldBeEqualTo ex

        source.removeAt(1)
        ex = (1..5).toList()
        result.data.size shouldBeEqualTo 5
        result.data.items.toList() shouldBeEqualTo ex

        source.add(source2.connect())
        ex = listOf(1, 2)
        result.data.size shouldBeEqualTo 2
        result.data.toList() shouldBeEqualTo ex

        //remove root except
        source.removeAt(0)
        ex = (100..104).toList()
        result.data.size shouldBeEqualTo 5
        result.data.items.toList() shouldBeEqualTo ex
    }
}
