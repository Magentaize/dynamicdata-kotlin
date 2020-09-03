package dynamicdata.list

import dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class AndFixture { //: AndFixtureBase{
    @Test
    fun t() {
        ItemChange.empty<String>() shouldBe ItemChange.empty()
        ItemChange.empty<Int>() shouldBeEqualTo ItemChange.empty<String>()
    }
}

internal abstract class AndFixtureBase : Disposable {
    protected val source1 = SourceList<Int>()
    protected val source2 = SourceList<Int>()
    private val results = createObservable().asAggregator()

    protected abstract fun createObservable(): Observable<IChangeSet<Int>>

    fun Dispose() {
        source1.dispose()
        source2.dispose()
        results.dispose()
    }

    @Test
    fun excludedWhenItemIsInOneSource(){
        source1.add(1)
    }
}
