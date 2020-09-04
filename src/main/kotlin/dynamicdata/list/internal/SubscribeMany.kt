package dynamicdata.list.internal

import dynamicdata.list.IChangeSet
import dynamicdata.list.disposeMany
import dynamicdata.list.transform
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

internal class SubscribeMany<T>(
    private val _source: Observable<IChangeSet<T>>,
    private val _subscriptionFactory: (T) -> Disposable
) {
    fun run(): Observable<IChangeSet<T>> =
        Observable.create {
            val shared = _source.publish()
            val subscriptions = shared
                .transform({ t -> _subscriptionFactory(t) })
                .disposeMany()
                .subscribe()

            CompositeDisposable(
                subscriptions,
                shared.subscribe(it::onNext, it::onError, it::onComplete),
                shared.connect()
            )
        }
}
