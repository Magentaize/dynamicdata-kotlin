package dynamicdata.list

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class MergeManyFixture {
    private val source = SourceList<ObjectWithObservable>()

    @Test
    fun invocationOnlyWhenChildIsInvoked() {
        var invoked = false
        source.connect()
            .mergeMany { it.observable }
            .subscribe { invoked = true }

        val item = ObjectWithObservable(1)

        source.add(item)
        source.remove(item)
        invoked shouldBeEqualTo false

        item.invokeObservable(true)
        invoked shouldBeEqualTo false
    }

    @Test
    fun everythingIsUnsubscribedWhenStreamIsDisposed() {
        var invoked = false
        val stream = source.connect()
            .mergeMany { it.observable }
            .subscribe { invoked = true }

        val item = ObjectWithObservable(1)

        source.add(item)
        stream.dispose()
        item.invokeObservable(true)

        invoked shouldBeEqualTo false
    }

    private class ObjectWithObservable(val id: Int) {
        private val _changed = PublishSubject.create<Boolean>()
        val observable: Observable<Boolean>
            get() = _changed

        fun invokeObservable(value: Boolean) {
            _changed.onNext(value)
        }
    }
}
