package xyz.magentaize.dynamicdata.cache.internal

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import xyz.magentaize.dynamicdata.cache.ChangeReason
import xyz.magentaize.dynamicdata.cache.ChangeSet
import xyz.magentaize.dynamicdata.kernel.ifHasValue
import xyz.magentaize.dynamicdata.kernel.subscribeBy

internal class DisposeMany<K, V>(
    private val _source: Observable<ChangeSet<K, V>>,
    private val _removeAction: (V) -> Unit
) {
    fun run(): Observable<ChangeSet<K, V>> =
        Observable.create { emitter ->
            val cache = Cache<K, V>()
            val subscriber = _source
                .doOnEach(object : Observer<ChangeSet<K, V>> {
                    override fun onNext(it: ChangeSet<K, V>) {
                        registerForRemoval(it, cache)
                    }

                    override fun onError(e: Throwable?) {
                        emitter.onError(e)
                    }

                    override fun onSubscribe(d: Disposable?) {
                    }

                    override fun onComplete() {
                    }
                })
                .subscribeBy(emitter)

            emitter.setDisposable(Disposable.fromAction {
                subscriber.dispose()
                cache.items.forEach(_removeAction)
                cache.clear()
            })
        }

    private fun registerForRemoval(changes: ChangeSet<K, V>, cache: Cache<K, V>) =
        changes.forEach {
            when (it.reason) {
                ChangeReason.Update ->
                    it.previous.ifHasValue(_removeAction)

                ChangeReason.Remove ->
                    _removeAction(it.current)
            }
        }
}