package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.kernel.subscribeBy
import xyz.magentaize.dynamicdata.list.ChangeSet
import xyz.magentaize.dynamicdata.list.ObservableList
import xyz.magentaize.dynamicdata.list.asObservableList
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable

internal class RefCount<T>(
    private val _source: Observable<ChangeSet<T>>
) {
    private val _lock = Any()
    private var _refCount = 0
    private var _list: ObservableList<T> = ObservableList.empty()

    fun run(): Observable<ChangeSet<T>> =
        Observable.create { emitter ->
            synchronized(_lock) {
                if (++_refCount == 1)
                    _list = _source.asObservableList()
            }

            val subscriber = _list.connect().subscribeBy(emitter)

            val d = Disposable.fromAction {
                subscriber.dispose()
                synchronized(_lock) {
                    if (--_refCount == 0) {
                        _list.dispose()
                    }
                }
            }

            emitter.setDisposable(d)
        }
}
