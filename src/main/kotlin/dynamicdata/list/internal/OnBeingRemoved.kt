package dynamicdata.list.internal

import dynamicdata.kernel.ifHasValue
import dynamicdata.kernel.subscribeBy
import dynamicdata.kernel.doOnEach
import dynamicdata.list.IChangeSet
import dynamicdata.list.ListChangeReason
import dynamicdata.list.clone
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable

internal class OnBeingRemoved<T>(
    private val _source: Observable<IChangeSet<T>>,
    private val _callback: (T) -> Unit
) {
    fun run(): Observable<IChangeSet<T>> =
        Observable.create { emitter ->
            val items = mutableListOf<T>()
            val subscription = _source
                .serialize()
                .doOnEach({ registerForRemoval(items, it) }, { e -> emitter.onError(e) })
                .subscribeBy(emitter)

            Disposable.fromAction {
                subscription.dispose()
                items.forEach(_callback)
            }
        }

    private fun registerForRemoval(items: MutableList<T>, changes: IChangeSet<T>) {
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
            }
        }

        return items.clone(changes)
    }
}
