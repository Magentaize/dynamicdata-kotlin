package xyz.magentaize.dynamicdata.cache.internal

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import xyz.magentaize.dynamicdata.cache.ChangeSet
import xyz.magentaize.dynamicdata.cache.disposeMany
import xyz.magentaize.dynamicdata.cache.transform
import xyz.magentaize.dynamicdata.kernel.subscribeBy

internal class SubscribeMany<K, V>(
    private val _source: Observable<ChangeSet<K, V>>,
    private val _factory: (K, V) -> Disposable
) {
    constructor(source: Observable<ChangeSet<K, V>>, factory: (V) -> Disposable) :
            this(source, { _, v -> factory(v) })

    fun run(): Observable<ChangeSet<K, V>> =
        Observable.create { emitter ->
            val published = _source.publish()
            val sub = published.transform(_factory)
                .disposeMany()
                .subscribe()

            emitter.setDisposable(
                CompositeDisposable(
                    sub,
                    published.subscribeBy(emitter),
                    published.connect()
                )
            )
        }
}