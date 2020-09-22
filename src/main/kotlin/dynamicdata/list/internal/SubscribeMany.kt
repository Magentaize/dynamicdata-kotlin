package dynamicdata.list.internal

import dynamicdata.kernel.subscribeBy
import dynamicdata.list.ChangeSet
import dynamicdata.list.disposeMany
import dynamicdata.list.transform
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

internal class SubscribeMany<T>(
    private val _source: Observable<ChangeSet<T>>,
    private val _subscriptionFactory: (T) -> Disposable
) {
    fun run(): Observable<ChangeSet<T>> =
        Observable.create { emitter ->
            val shared = _source.publish()
            val subscriptions = shared
                .transform { t -> _subscriptionFactory(t) }
                .disposeMany()
                .subscribe()

            val d = CompositeDisposable(
                subscriptions,
                shared.subscribeBy(emitter),
                shared.connect()
            )

            emitter.setDisposable(d)
        }
}
