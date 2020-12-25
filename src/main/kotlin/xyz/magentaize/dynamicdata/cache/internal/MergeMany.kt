package xyz.magentaize.dynamicdata.cache.internal

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.internal.functions.Functions
import xyz.magentaize.dynamicdata.cache.ChangeSet
import xyz.magentaize.dynamicdata.cache.subscribeMany

internal class MergeMany<K, V, R>(
    val _source: Observable<ChangeSet<K, V>>,
    val _selector: (K, V) -> Observable<R>
) {
    constructor(source: Observable<ChangeSet<K, V>>, selector: (V) -> Observable<R>) :
            this(source, { _, t -> selector(t) })

    fun run(): Observable<R> =
        Observable.create { emitter ->
            _source.subscribeMany { k, v ->
                _selector(k, v).subscribe(emitter::onNext, Functions.emptyConsumer(), Functions.EMPTY_ACTION)
            }
                .subscribe(Functions.emptyConsumer(), emitter::onError)
        }
}