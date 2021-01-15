package xyz.magentaize.dynamicdata.cache

import io.reactivex.rxjava3.core.Observable
import org.amshove.kluent.shouldBeEqualTo
import xyz.magentaize.dynamicdata.domain.Person
import kotlin.test.Test

internal class ExceptFixture : ExceptFixtureBase() {
    override fun createObservable(): Observable<ChangeSet<String, Person>> {
        return targetSource.connect().except(exceptSource.connect())
    }
}

internal class ExceptCollectionFixture : ExceptFixtureBase() {
    override fun createObservable(): Observable<ChangeSet<String, Person>> {
        val ret = listOf(targetSource.connect(), exceptSource.connect())
        return ret.except()
    }
}

internal abstract class ExceptFixtureBase {
    protected val targetSource = SourceCache<String, Person> { it.name }
    protected val exceptSource = SourceCache<String, Person> { it.name }
    private val results = createObservable().asAggregator()

    protected abstract fun createObservable(): Observable<ChangeSet<String, Person>>

    @Test
    fun doNotIncludeExceptListItems() {
        val p = Person("Adult1", 50)
        exceptSource.addOrUpdate(p)
        targetSource.addOrUpdate(p)

        results.messages.size shouldBeEqualTo 0
        results.data.size shouldBeEqualTo 0
    }

    @Test
    fun removedAnItemFromExceptThenIncludesTheItem() {
        val p = Person("Adult1", 50)
        exceptSource.addOrUpdate(p)
        targetSource.addOrUpdate(p)
        exceptSource.removeItem(p)

        results.messages.size shouldBeEqualTo 1
        results.data.size shouldBeEqualTo 1
    }

    @Test
    fun updatingOneSourceOnlyProducesResult() {
        val p = Person("Adult1", 50)
        targetSource.addOrUpdate(p)

        results.messages.size shouldBeEqualTo 1
        results.data.size shouldBeEqualTo 1
    }
}
