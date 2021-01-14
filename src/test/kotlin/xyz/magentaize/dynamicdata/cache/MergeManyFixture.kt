package xyz.magentaize.dynamicdata.cache

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

class MergeManyFixture {
    private val source = SourceCache<Int, ObjectWithObservable> { it.id }

    @Test
    fun everythingIsUnsubscribedWhenStreamIsDisposed() {
        var invoked = false
        val stream = source.connect()
            .mergeMany { it -> it.observable }
            .subscribe { invoked = true }
        val item = ObjectWithObservable(1)
        source.addOrUpdate(item)
        source.dispose()
        item.invokeObservable(true)

        invoked shouldBeEqualTo false
    }

    @Test
    fun invocationOnlyWhenChildIsInvoked() {
        var invoked = false
        val stream = source.connect().mergeMany { it -> it.observable }.subscribe { invoked = true }
        val item = ObjectWithObservable(1)
        source.addOrUpdate(item)
        invoked shouldBeEqualTo false

        item.invokeObservable(true)
        invoked shouldBeEqualTo true

        stream.dispose()
    }

    @Test
    fun removedItemWillNotCauseInvocation() {
        var invoked = false
        val stream = source.connect().mergeMany { it -> it.observable }.subscribe { invoked = true }
        val item = ObjectWithObservable(1)
        source.addOrUpdate(item)
        source.removeItem(item)
        invoked shouldBeEqualTo false

        item.invokeObservable(true)
        invoked shouldBeEqualTo false

        stream.dispose()
    }

    private class ObjectWithObservable(val id: Int) {
        private val _changed = PublishSubject.create<Boolean>()

        val observable: Observable<Boolean>
            get() = _changed

        fun invokeObservable(value: Boolean) =
            _changed.onNext(value)
    }
}
