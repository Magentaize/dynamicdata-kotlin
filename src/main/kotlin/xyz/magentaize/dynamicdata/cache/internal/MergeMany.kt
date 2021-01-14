package xyz.magentaize.dynamicdata.cache.internal

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.internal.functions.Functions
import xyz.magentaize.dynamicdata.cache.ChangeSet
import xyz.magentaize.dynamicdata.cache.subscribeMany
import xyz.magentaize.dynamicdata.kernel.ObservableEx

internal class MergeMany<K, V, R>(
    private val _source: Observable<ChangeSet<K, V>>,
    private val _selector: (K, V) -> Observable<R>
) {
    constructor(source: Observable<ChangeSet<K, V>>, selector: (V) -> Observable<R>) :
            this(source, { _, t -> selector(t) })

    fun run(): Observable<R> =
        ObservableEx.create { emitter ->
            return@create _source
                .subscribeMany { k, v ->
                    _selector(k, v)
                        .subscribe(emitter::onNext)
                }
                .subscribe(Functions.emptyConsumer(), emitter::onError)
        }
}
