package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.kernel.Optional
import xyz.magentaize.dynamicdata.kernel.convertOr
import xyz.magentaize.dynamicdata.kernel.subscribeBy
import xyz.magentaize.dynamicdata.kernel.valueOrThrow
import xyz.magentaize.dynamicdata.list.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.internal.functions.Functions

internal class Filter<T>(
    private val _source: Observable<ChangeSet<T>>,
    private val _policy: ListFilterPolicy
) {
    private lateinit var _predicate: (T) -> Boolean
    private lateinit var _predicates: Observable<(T) -> Boolean>

    constructor(
        source: Observable<ChangeSet<T>>,
        predicates: Observable<(T) -> Boolean>,
        policy: ListFilterPolicy = ListFilterPolicy.CalculateDiff
    ) : this(source, policy) {
        _predicates = predicates
    }

    constructor(
        source: Observable<ChangeSet<T>>,
        predicate: (T) -> Boolean,
        policy: ListFilterPolicy = ListFilterPolicy.CalculateDiff
    ) : this(source, policy) {
        _predicate = predicate
    }

    fun run(): Observable<ChangeSet<T>> =
        Observable.create { emitter ->
            var predicate = { _: T -> false }
            val all = mutableListOf<ItemWithMatch<T>>()
            val filtered = ChangeAwareList<ItemWithMatch<T>>()
            val immutableFilter = this::_predicate.isInitialized
            val predicateChanged: Observable<ChangeSet<ItemWithMatch<T>>>

            if (immutableFilter) {
                predicateChanged = Observable.never()
                predicate = _predicate
            } else {
                predicateChanged = _predicates
                    .serialize()
                    .map {
                        predicate = it
                        requery(predicate, all, filtered)
                    }
            }

            /*
                * Apply the transform operator so 'IsMatch' state can be evaluated and captured one time only
                * This is to eliminate the need to re-apply the predicate when determining whether an item was previously matched,
                * which is essential when we have mutable state
                */

            //Need to get item by index and store it in the transform
            val filteredResult = _source
                .serialize()
                .transformWithOptional({ t, prev: Optional<ItemWithMatch<T>> ->
                    val wasMatch = prev.convertOr({ p -> p.isMatch }, { false })
                    ItemWithMatch(t, predicate(t), wasMatch)
                }, true)
                .map { changes ->
                    //keep track of all changes if filtering on an observable
                    if (!immutableFilter)
                        all.clone(changes)

                    return@map process(filtered, changes)
                }

            val d = predicateChanged.mergeWith(filteredResult)
                .notEmpty()
                .map { it.transform { iwm -> iwm.item } }
                .subscribeBy(emitter)

            emitter.setDisposable(d)
        }

    private fun requery(
        predicate: (T) -> Boolean,
        all: MutableList<ItemWithMatch<T>>,
        filtered: ChangeAwareList<ItemWithMatch<T>>
    ): ChangeSet<ItemWithMatch<T>> {
        if (all.size == 0)
            return AnonymousChangeSet.empty()

        if (_policy == ListFilterPolicy.ClearAndReplace) {
            val itemsWithMatch = all.map { ItemWithMatch(it.item, predicate(it.item), it.isMatch) }

            //mark items as matched?
            filtered.clear()
            filtered.addAll(itemsWithMatch.filter { it.isMatch })

            //reset state for all items
            all.clear()
            all.addAll(itemsWithMatch)
            return filtered.captureChanges()
        }

        val toAdd = ArrayList<ItemWithMatch<T>>(all.size)
        val toRemove = ArrayList<ItemWithMatch<T>>(all.size)

        (0 until all.size).forEach { i ->
            val original = all[i]
            val newItem = ItemWithMatch(original.item, predicate(original.item), original.isMatch)
            all[i] = newItem

            if (newItem.isMatch && !newItem.wasMatch)
                toAdd.add(newItem)
            else if (!newItem.isMatch && newItem.wasMatch)
                toRemove.add(newItem)
        }

        filtered.removeAll(toRemove)
        filtered.addAll(toAdd)

        return filtered.captureChanges()
    }

    private fun process(
        filtered: ChangeAwareList<ItemWithMatch<T>>,
        changes: ChangeSet<ItemWithMatch<T>>
    ): ChangeSet<ItemWithMatch<T>> {
        //Maintain all items as well as filtered list. This enables us to a) requery when the predicate changes b) check the previous state when Refresh is called
        changes.forEach {
            val change = it.item

            when (it.reason) {
                ListChangeReason.Add ->
                    if (change.current.isMatch)
                        filtered.add(change.current)

                ListChangeReason.AddRange -> {
                    val matches = it.range.filter { t -> t.isMatch }.toList()
                    filtered.addAll(matches)
                }

                ListChangeReason.Replace -> {
                    val match = change.current.isMatch
                    val wasMatch = it.item.current.wasMatch
                    if (match) {
                        if (wasMatch) {
                            //an update, so get the latest index and pass the index up the chain
                            val prev = filtered.map { x -> x.item }
                                .indexOfOptional(change.previous.value.item)
                                .valueOrThrow { IllegalStateException("Cannot find index of -> ${change.previous.value}. Expected to be in the list.") }

                            //replace inline
                            filtered[prev.index] = change.current
                        } else {
                            filtered.add(change.current)
                        }
                    } else {
                        if (wasMatch)
                            filtered.remove(change.previous.value)
                    }
                }

                ListChangeReason.Refresh -> {
                    val match = change.current.isMatch
                    val wasMatch = it.item.current.wasMatch

                    if (match) {
                        if (wasMatch) {
                            //an update, so get the latest index and pass the index up the chain
                            val prev = filtered.map { x -> x.item }
                                .indexOfOptional(change.current.item)
                                .valueOrThrow { IllegalStateException("Cannot find index of -> ${change.current.item}. Expected to be in the list.") }

                            filtered.refreshAt(prev.index)
                        } else {
                            filtered.add(change.current)
                        }
                    } else {
                        if (wasMatch)
                            filtered.remove(change.previous.value)
                    }
                }

                ListChangeReason.Remove ->
                    filtered.remove(it.item.current)

                ListChangeReason.RemoveRange ->
                    filtered.removeMany(it.range)

                ListChangeReason.Clear ->
                    filtered.clearOrRemoveMany(it)

                else -> Functions.EMPTY_ACTION
            }
        }

        return filtered.captureChanges()
    }

    data class ItemWithMatch<T>(
        val item: T,
        val isMatch: Boolean,
        val wasMatch: Boolean
    ) {
        override fun equals(other: Any?): Boolean {
            return if (other is ItemWithMatch<*>)
                item == other.item
            else
                false
        }
    }
}
