package dynamicdata.list.internal

import dynamicdata.kernel.subscribeBy
import dynamicdata.list.ChangeSet
import dynamicdata.list.ObservableList
import dynamicdata.list.asObservableList
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable

internal class RefCount<T>(
    private val _source: Observable<ChangeSet<T>>
) {
    private val _lock = Any()
    private var _refCount = 0
    private var _list: ObservableList<T>? = null

    fun run(): Observable<ChangeSet<T>> =
        Observable.create { emitter ->
            synchronized(_lock) {
                if (++_refCount == 1)
                    _list = _source.asObservableList()
            }

            val subscriber = _list!!.connect().subscribeBy(emitter)

            val d = Disposable.fromAction {
                subscriber.dispose()
                var listToDispose: Disposable? = null
                synchronized(_lock) {
                    if (--_refCount == 0) {
                        listToDispose = _list
                        _list = null
                    }
                }

                listToDispose?.dispose()
            }

            emitter.setDisposable(d)
        }
}
