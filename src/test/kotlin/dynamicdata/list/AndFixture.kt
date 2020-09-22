package dynamicdata.list

import dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.core.Observable
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class AndFixture : AndFixtureBase() {
    override fun createObservable(): Observable<ChangeSet<Int>> {
        return source1.connect().and(source2.connect())
    }
}

internal class AndCollectionFixture : AndFixtureBase() {
    override fun createObservable(): Observable<ChangeSet<Int>> {
        val ret = listOf(source1.connect(), source2.connect())
        return ret.and()
    }
}

internal abstract class AndFixtureBase {
    protected val source1 = SourceList<Int>()
    protected val source2 = SourceList<Int>()
    private val results = createObservable().asAggregator()

    protected abstract fun createObservable(): Observable<ChangeSet<Int>>

//    fun Dispose() {
//        source1.dispose()
//        source2.dispose()
//        results.dispose()
//    }

    //@Ignore
    @Test
    fun excludedWhenItemIsInOneSource() {
        source1.add(1)
        results.data.size shouldBeEqualTo 0
    }

    @Test
    fun includedWhenItemIsInTwoSources(){
        source1.add(1)
        source2.add(1)
        results.data.size shouldBeEqualTo 1
    }

    @Test
    fun removedWhenNoLongerInBoth(){
        source1.add(1)
        source2.add(1)
        source1.remove(1)
        results.data.size shouldBeEqualTo 0
    }

    @Test
    fun combineRange(){
        source1.addRange(1..10)
        source2.addRange(6..15)
        results.data.size shouldBeEqualTo 5
        results.data.items.toList() shouldBeEqualTo (6..10).toList()
    }

    @Test
    fun clearOneClearsResult(){
        source1.addRange(1..5)
        source2.addRange(1..5)
        source1.clear()
        results.data.size shouldBeEqualTo 0
    }

    @Test
    fun startingWithNonEmptySourceProducesNoResult(){
        source1.add(1)
        val result = createObservable().asAggregator()
        result.data.size shouldBeEqualTo 0
    }
}
