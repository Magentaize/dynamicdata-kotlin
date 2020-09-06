package dynamicdata.list.internal

import dynamicdata.list.IChangeSet
import dynamicdata.list.subscribeMany
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.internal.functions.Functions

internal class MergeMany<T, R>(
    private val _source: Observable<IChangeSet<T>>,
    private val _selector: (T) -> Observable<R>
) {
    fun run(): Observable<R> =
        Observable.create { emitter ->
            val d = _source
                .subscribeMany { t ->
                _selector(t).serialize().subscribe(emitter::onNext)
            }
                .subscribe(Functions.emptyConsumer(), emitter::onError)

            emitter.setDisposable(d)
        }
}
