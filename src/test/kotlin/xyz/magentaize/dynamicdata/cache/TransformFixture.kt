package xyz.magentaize.dynamicdata.cache

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import org.amshove.kluent.`should contain same`
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBe
import xyz.magentaize.dynamicdata.cache.test.ChangeSetAggregator
import xyz.magentaize.dynamicdata.domain.Person
import xyz.magentaize.dynamicdata.domain.PersonWithGender
import kotlin.test.Test

class TransformFixture {

    @Test
    fun add() {
        val stub = TransformStub()
        val p = Person("Adult1", 50)
        stub.source.addOrUpdate(p)

        stub.result.messages.size shouldBeEqualTo 1
        stub.result.data.size shouldBeEqualTo 1
        stub.result.data.items.first() shouldBeEqualTo stub.transformFactory(p)
    }

    @Test
    fun batchOfUniqueUpdates() {
        val p = Person.make100AgedPeople()
        val stub = TransformStub()
        stub.source.addOrUpdate(p)

        stub.result.messages.size shouldBeEqualTo 1
        stub.result.messages[0].adds shouldBeEqualTo 100

        val t = p.map(stub.transformFactory).sortedBy { it.age }.toList()
        stub.result.data.items.sortedBy { it.name } `should contain same` t
    }

    @Test
    fun clear() {
        val stub = TransformStub()
        val p = Person.make100AgedPeople()
        stub.source.addOrUpdate(p)
        stub.source.clear()

        stub.result.messages.size shouldBeEqualTo 2
        stub.result.messages[0].adds shouldBeEqualTo 100
        stub.result.messages[1].removes shouldBeEqualTo 100
        stub.result.data.size shouldBeEqualTo 0
    }

    @Test
    fun remove() {
        val key = "Adult1"
        val p = Person(key, 50)
        val stub = TransformStub()
        stub.source.addOrUpdate(p)
        stub.source.remove(key)

        stub.result.messages.size shouldBeEqualTo 2
        stub.result.messages[0].adds shouldBeEqualTo 1
        stub.result.messages[1].removes shouldBeEqualTo 1
        stub.result.data.size shouldBeEqualTo 0
    }

    @Test
    fun reTransformAll() {
        val p = (1..10).map { Person("Name$it", it) }.toList()
        val forceTransform = PublishSubject.create<Unit>()
        val stub = TransformStub(forceTransform)
        stub.source.addOrUpdate(p)
        forceTransform.onNext(Unit)

        stub.result.messages.size shouldBeEqualTo 2
        stub.result.messages[1].updates shouldBeEqualTo 10
        (1..10).forEach {
            val original = stub.result.messages[0].elementAt(it - 1).current
            val updated = stub.result.messages[1].elementAt(it - 1).current
            updated shouldBeEqualTo original
            original shouldNotBe updated
        }
    }

    @Test
    fun reTransformSelected() {
        val p = Person.make100AgedPeople()
        val forceTransform = PublishSubject.create<(Person) -> Boolean>()
        val stub = TransformStub(forceTransform)
        stub.source.addOrUpdate(p)
        forceTransform.onNext { it.age <= 5 }

        stub.result.messages.size shouldBeEqualTo 2
        stub.result.messages[1].updates shouldBeEqualTo 5
        (1..5).forEach {
            val original = stub.result.messages[0].elementAt(it - 1).current
            val updated = stub.result.messages[1].elementAt(it - 1).current
            updated shouldBeEqualTo original
            original shouldNotBe updated
        }
    }

    @Test
    fun sameKeyChanges() {
        val stub = TransformStub()
        val p = (1..10).map { Person("Name", it) }.toList()
        stub.source.addOrUpdate(p)

        stub.result.messages.size shouldBeEqualTo 1
        stub.result.messages[0].adds shouldBeEqualTo 1
        stub.result.messages[0].updates shouldBeEqualTo 9
        stub.result.data.size shouldBeEqualTo 1

        val last = stub.transformFactory(p.last())
        val item = stub.result.data.items.first()

        last shouldBeEqualTo item
    }

    @Test
    fun transformToNull() {
        val source = SourceCache<String, Person> { it.name }
        val result =
            ChangeSetAggregator(
                source.connect().transform<String, Person, PersonWithGender?>(factory = { _: Person -> null })
            )
        source.addOrUpdate(Person("Name", 50))

        result.messages.size shouldBeEqualTo 1
        result.data.size shouldBeEqualTo 1
        result.data.items.first() shouldBeEqualTo null
    }

    @Test
    fun update() {
        val key = "Adult1"
        val new = Person(key, 50)
        val updated = Person(key, 51)
        val stub = TransformStub()
        stub.source.addOrUpdate(new)
        stub.source.addOrUpdate(updated)

        stub.result.messages.size shouldBeEqualTo 2
        stub.result.messages[0].adds shouldBeEqualTo 1
        stub.result.messages[1].updates shouldBeEqualTo 1
    }

    private class TransformStub(
        val source: SourceCache<String, Person>,
        val result: ChangeSetAggregator<String, PersonWithGender>
    ) : Disposable {
        companion object {
            val transformFactory = { it: Person -> PersonWithGender(it, if (it.age % 2 == 0) "M" else "F") }

            operator fun invoke(): TransformStub {
                val source = SourceCache<String, Person> { it.name }
                return TransformStub(
                    source,
                    ChangeSetAggregator(source.connect().transform(transformFactory))
                )
            }

            @JvmName("invokeWithUnit")
            operator fun invoke(reTransformer: Observable<Unit>): TransformStub {
                val source = SourceCache<String, Person> { it.name }
                return TransformStub(
                    source,
                    ChangeSetAggregator(source.connect().transform(transformFactory, reTransformer))
                )
            }

            operator fun invoke(reTransformer: Observable<(Person) -> Boolean>): TransformStub {
                val source = SourceCache<String, Person> { it.name }
                return TransformStub(
                    source,
                    ChangeSetAggregator(source.connect().transform(transformFactory, reTransformer))
                )
            }
        }

        val transformFactory = { it: Person -> PersonWithGender(it, if (it.age % 2 == 0) "M" else "F") }

        private var _isDisposed = false

        override fun dispose() {
            source.dispose()
            result.dispose()
            _isDisposed = true
        }

        override fun isDisposed(): Boolean = _isDisposed
    }
}

