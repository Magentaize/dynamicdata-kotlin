package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.list.*
import io.reactivex.rxjava3.core.Observable
import xyz.magentaize.dynamicdata.kernel.*
import xyz.magentaize.dynamicdata.kernel.subscribeBy

internal class Distinct<T, R>(
    private val _source: Observable<ChangeSet<T>>,
    private val _selector: (T) -> R
) {
    fun run(): Observable<ChangeSet<R>> =
        ObservableEx.create { emitter ->
            val valueCounters = mutableMapOf<R, Int>()
            val result = ChangeAwareList<R>()

            return@create _source.transformWithOptional<T, ItemWithMatch<T, R>>({ t, prev, _ ->
                val previousValue = prev.convertOr({ it.value }, { null as R })
                ItemWithMatch(t, _selector(t), previousValue)
            }, true)
                .map { changes -> process(valueCounters, result, changes) }
                .notEmpty()
                .subscribeBy(emitter)
        }

    private fun process(
        values: MutableMap<R, Int>,
        result: ChangeAwareList<R>,
        changes: ChangeSet<ItemWithMatch<T, R>>
    ): ChangeSet<R> {
        fun addAction(value: R) = values.lookup(value)
            .ifHasValue { count -> values[value] = count + 1 }
            .`else` {
                values[value] = 1
                result.add(value)
            }

        fun removeAction(value: R) {
            val counter = values.lookup(value)

            if (!counter.hasValue) return

            //decrement counter
            val newCount = counter.value - 1
            values[value] = newCount

            if (newCount != 0) return

            //if there are none, then remove and notify
            result.remove(value)
            values.remove(value)
        }

        changes.forEach { change ->
            when (change.reason) {
                ListChangeReason.Add -> {
                    val value = change.item.current.value
                    addAction(value)
                }
                ListChangeReason.AddRange ->
                    change.range.map { it.value }.forEach { addAction(it) }
                ListChangeReason.Refresh -> {
                    val value = change.item.current.value
                    val prev = change.item.current.previous
                    if (value != prev) {
                        removeAction(prev)
                        addAction(value)
                    }
                }
                ListChangeReason.Replace -> {
                    val value = change.item.current.value
                    val prev = change.item.previous.value.value
                    if (value != prev) {
                        removeAction(prev)
                        addAction(value)
                    }
                }
                ListChangeReason.Remove -> {
                    val prev = change.item.current.value
                    removeAction(prev)
                }
                ListChangeReason.RemoveRange ->
                    change.range.map { it.value }.forEach { removeAction(it) }
                ListChangeReason.Clear -> {
                    result.clear()
                    values.clear()
                }
            }
        }

        return result.captureChanges()
    }

    private data class ItemWithMatch<T, R>(
        val item: T,
        val value: R,
        val previous: R
    ){
        override fun equals(other: Any?): Boolean {
            return if(other is ItemWithMatch<*, *>)
                item == other.item
            else
                false
        }

        override fun toString(): String {
            return "item: $item, value: $value, previous: $previous"
        }
    }
}
