package xyz.magentaize.dynamicdata.cache.internal

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.internal.functions.Functions
import xyz.magentaize.dynamicdata.cache.ChangeReason
import xyz.magentaize.dynamicdata.cache.ChangeSet
import xyz.magentaize.dynamicdata.kernel.ObservableEx
import xyz.magentaize.dynamicdata.kernel.doOnEach
import xyz.magentaize.dynamicdata.kernel.ifHasValue
import xyz.magentaize.dynamicdata.kernel.subscribeBy

internal class DisposeMany<K, V>(
    private val _source: Observable<ChangeSet<K, V>>,
    private val _removeAction: (V) -> Unit
) {
    fun run(): Observable<ChangeSet<K, V>> =
        ObservableEx.create { emitter ->
            val cache = Cache<K, V>()
            val subscriber = _source
                .doOnEach({ registerForRemoval(it, cache) }, { e -> emitter.onError(e) })
                .subscribeBy(emitter)

            return@create Disposable.fromAction {
                subscriber.dispose()
                cache.items.forEach(_removeAction)
                cache.clear()
            }
        }

    private fun registerForRemoval(changes: ChangeSet<K, V>, cache: Cache<K, V>) {
        changes.forEach {
            when (it.reason) {
                ChangeReason.Update ->
                    it.previous.ifHasValue(_removeAction)

                ChangeReason.Remove ->
                    _removeAction(it.current)

                else -> {}
            }
        }
        cache.clone(changes)
    }
}
