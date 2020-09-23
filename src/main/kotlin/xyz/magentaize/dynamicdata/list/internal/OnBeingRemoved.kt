package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.kernel.ifHasValue
import xyz.magentaize.dynamicdata.kernel.subscribeBy
import xyz.magentaize.dynamicdata.kernel.doOnEach
import xyz.magentaize.dynamicdata.list.ChangeSet
import xyz.magentaize.dynamicdata.list.ListChangeReason
import xyz.magentaize.dynamicdata.list.clone
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.internal.functions.Functions

internal class OnBeingRemoved<T>(
    private val _source: Observable<ChangeSet<T>>,
    private val _callback: (T) -> Unit
) {
    fun run(): Observable<ChangeSet<T>> =
        Observable.create { emitter ->
            val items = mutableListOf<T>()
            val subscription = _source
                .serialize()
                .doOnEach({ registerForRemoval(items, it) }, { e -> emitter.onError(e) })
                .subscribeBy(emitter)

            val d = Disposable.fromAction {
                subscription.dispose()
                items.forEach(_callback)
            }

            emitter.setDisposable(d)
        }

    private fun registerForRemoval(items: MutableList<T>, changes: ChangeSet<T>) {
        changes.forEach { change ->
            when (change.reason) {
                ListChangeReason.Replace ->
                    change.item.previous.ifHasValue(_callback)

                ListChangeReason.Remove ->
                    _callback(change.item.current)

                ListChangeReason.RemoveRange ->
                    change.range.forEach(_callback)

                ListChangeReason.Clear ->
                    items.forEach(_callback)

                else -> Functions.EMPTY_ACTION
            }
        }

        return items.clone(changes)
    }
}
