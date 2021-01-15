package xyz.magentaize.dynamicdata.cache

import io.reactivex.rxjava3.core.Observable
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import xyz.magentaize.dynamicdata.domain.Person
import kotlin.test.Test

internal class AndFixture : AndFixtureBase() {
    override fun createObservable(): Observable<ChangeSet<String, Person>> {
        return source1.connect().and(source2.connect())
    }
}

internal class AndCollectionFixture : AndFixtureBase() {
    override fun createObservable(): Observable<ChangeSet<String, Person>> {
        val ret = listOf(source1.connect(), source2.connect())
        return ret.and()
    }
}

internal abstract class AndFixtureBase {
    protected val source1 = SourceCache<String, Person> { it.name }
    protected val source2 = SourceCache<String, Person> { it.name }
    private val results = createObservable().asAggregator()

    protected abstract fun createObservable(): Observable<ChangeSet<String, Person>>

    @Test
    fun removingFromOneRemovesFromResult() {
        val p = Person("Adult1", 50)
        source1.addOrUpdate(p)
        source2.addOrUpdate(p)
        source2.removeItem(p)

        results.messages.size shouldBeEqualTo 2
        results.data.size shouldBeEqualTo 0
    }

    @Test
    fun startingWithNonEmptySourceProducesNoResult() {
        val p = Person("Adult1", 50)
        source1.addOrUpdate(p)
        val result2 = createObservable().asAggregator()

        results.messages.size shouldBeEqualTo 0
        result2.data.size shouldBeEqualTo 0
    }

    @Test
    fun updatingBothProducesResults() {
        val p = Person("Adult1", 50)
        source1.addOrUpdate(p)
        source2.addOrUpdate(p)

        results.messages.size shouldBeEqualTo 1
        results.data.size shouldBeEqualTo 1
        results.data.items.first() shouldBe p
    }

    @Test
    fun updatingOneProducesOnlyOneUpdate() {
        val p = Person("Adult1", 50)
        source1.addOrUpdate(p)
        source2.addOrUpdate(p)

        val updated = Person("Adult1", 51)
        source2.addOrUpdate(updated)

        results.messages.size shouldBeEqualTo 2
        results.data.size shouldBeEqualTo 1
        results.data.items.first() shouldBe updated
    }

    @Test
    fun updatingOneSourceOnlyProducesNoResults() {
        val p = Person("Adult1", 50)
        source1.addOrUpdate(p)

        results.messages.size shouldBeEqualTo 0
        results.data.size shouldBeEqualTo 0
    }
}
