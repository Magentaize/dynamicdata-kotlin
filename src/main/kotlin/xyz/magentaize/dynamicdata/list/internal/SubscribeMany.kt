package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.kernel.subscribeBy
import xyz.magentaize.dynamicdata.list.ChangeSet
import xyz.magentaize.dynamicdata.list.disposeMany
import xyz.magentaize.dynamicdata.list.transform
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import xyz.magentaize.dynamicdata.kernel.ObservableEx

internal class SubscribeMany<T>(
    private val _source: Observable<ChangeSet<T>>,
    private val _factory: (T) -> Disposable
) {
    fun run(): Observable<ChangeSet<T>> =
        ObservableEx.create { emitter ->
            val shared = _source.publish()
            val subscriptions = shared
                .transform(_factory)
                .disposeMany()
                .subscribe()

            return@create CompositeDisposable(
                subscriptions,
                shared.subscribeBy(emitter),
                shared.connect()
            )
        }
}
