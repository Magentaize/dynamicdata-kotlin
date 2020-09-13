package dynamicdata.list

import dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.core.Observable
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class ExceptFixture : ExceptFixtureBase() {
    override fun createObservable(): Observable<IChangeSet<Int>> =
        source1.connect().except(source2.connect())
}

internal class ExceptCollectionFixture : ExceptFixtureBase() {
    override fun createObservable(): Observable<IChangeSet<Int>> =
        listOf(source1.connect(), source2.connect()).except()
}

internal abstract class ExceptFixtureBase {
    protected val source1 = SourceList<Int>()
    protected val source2 = SourceList<Int>()
    private val result = createObservable().asAggregator()

    abstract fun createObservable(): Observable<IChangeSet<Int>>

    @Test
    fun includedWhenItemIsInOneSource() {
        source1.add(1)
        result.data.size shouldBeEqualTo 1
    }

    @Test
    fun nothingFromOther() {
        source2.add(1)
        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun excludedWhenItemIsInTwoSources() {
        source1.add(1)
        source2.add(1)
        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun addedWhenNoLongerInSecond() {
        source1.add(1)
        source2.add(1)
        source2.remove(1)
        result.data.size shouldBeEqualTo 1
    }

    @Test
    fun combineRange() {
        source1.addRange(1..10)
        source2.addRange(6..15)
        result.data.size shouldBeEqualTo 5
        result.data.items.toList() shouldBeEqualTo (1..5).toList()
    }

    @Test
    fun clearFirstClearsResult() {
        source1.addRange(1..5)
        source2.addRange(1..5)
        source1.clear()
        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun clearSecondEnsuresFirstIsIncluded() {
        source1.addRange(1..5)
        source2.addRange(1..5)
        result.data.size shouldBeEqualTo 0
        source2.clear()
        result.data.size shouldBeEqualTo 5
        result.data.items.toList() shouldBeEqualTo (1..5).toList()
    }
}
