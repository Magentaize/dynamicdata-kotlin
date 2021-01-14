package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.list.ChangeSet
import xyz.magentaize.dynamicdata.list.subscribeMany
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.internal.functions.Functions
import xyz.magentaize.dynamicdata.kernel.ObservableEx

internal class MergeMany<T, R>(
    private val _source: Observable<ChangeSet<T>>,
    private val _selector: (T) -> Observable<R>
) {
    fun run(): Observable<R> =
        ObservableEx.create { emitter ->
            return@create _source
                .subscribeMany { t ->
                    _selector(t).serialize().subscribe(emitter::onNext)
                }
                .subscribe(Functions.emptyConsumer(), emitter::onError)
        }
}
