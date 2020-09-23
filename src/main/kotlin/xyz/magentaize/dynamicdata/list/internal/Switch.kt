package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.kernel.subscribeBy
import xyz.magentaize.dynamicdata.list.ChangeSet
import xyz.magentaize.dynamicdata.list.SourceList
import xyz.magentaize.dynamicdata.list.clear
import xyz.magentaize.dynamicdata.list.populateInto
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.internal.functions.Functions

internal class Switch<T>(
    private var _source: Observable<Observable<ChangeSet<T>>>
) {
    fun run(): Observable<ChangeSet<T>> =
        Observable.create { emitter ->
            val dest = SourceList<T>()

            val populator = _source
                .doOnEach { dest.clear() }
                .switchMap(Functions.identity())
                .populateInto(dest)

            val publisher = dest.connect().subscribeBy(emitter)

            val d = CompositeDisposable(dest, populator, publisher)

            emitter.setDisposable(d)
        }
}
