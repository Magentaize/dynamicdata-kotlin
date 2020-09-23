package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.kernel.subscribeBy
import xyz.magentaize.dynamicdata.kernel.valueOrThrow
import xyz.magentaize.dynamicdata.list.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.internal.functions.Functions

internal class Sort<T>(
    private val _source: Observable<ChangeSet<T>>,
    private var _comparator: Comparator<T>,
    private val _sortOption: SortOption,
    private val _resort: Observable<Unit>,
    private val _comparatorChanged: Observable<Comparator<T>>,
    private val _resetThreshold: Int
) {

    fun run(): Observable<ChangeSet<T>> =
        Observable.create { emitter ->
            val original = mutableListOf<T>()
            val target = ChangeAwareList<T>()
            val changed = _source.serialize()
                .map { changes ->
                    if (_resetThreshold > 1)
                        original.clone(changes)

                    if (changes.totalChanges > _resetThreshold)
                        reset(original, target)
                    else
                        process(target, changes)
                }
            val resort = _resort.serialize()
                .map { reorder(target) }
            val changeComparer = _comparatorChanged.serialize()
                .map { changeComparer(target, it) }

            val d = changed.mergeWith(resort)
                .mergeWith(changeComparer)
                .filter { change -> change.size != 0 }
                .subscribeBy(emitter)

            emitter.setDisposable(d)
        }

    private fun changeComparer(target: ChangeAwareList<T>, comparator: Comparator<T>): ChangeSet<T> {
        _comparator = comparator

        if (_resetThreshold > 0 && target.size <= _resetThreshold)
            return reorder(target)

        val sorted = target.sortedWith(_comparator).toList()
        target.clear()
        target.addAll(sorted)

        return target.captureChanges()
    }

    private fun reset(original: MutableList<T>, target: ChangeAwareList<T>): ChangeSet<T> {
        val sorted = original.sortedWith(_comparator).toList()
        target.clear()
        target.addAll(sorted)

        return target.captureChanges()
    }

    private fun reorder(target: ChangeAwareList<T>): ChangeSet<T> {
        var index = -1
        val sorted = target.sortedWith(_comparator).toList()
        sorted.forEach {
            index++

            val existing = target[index]
            //if item is in the same place,
            if (it === existing) return@forEach

            //Cannot use binary search as Resort is implicit of a mutable change
            val old = target.indexOf(it)
            target.move(old, index)
        }

        return target.captureChanges()
    }

    private fun process(target: ChangeAwareList<T>, changes: ChangeSet<T>): ChangeSet<T> {
        //if all removes and not Clear, then more efficient to try clear range
        if (changes.totalChanges == changes.removes
            && changes.all { it.reason != ListChangeReason.Clear }
            && changes.removes > 1
        ) {
            val removed = changes.unified().map { it.current }
            target.removeMany(removed)

            return target.captureChanges()
        }

        return processImpl(target, changes)
    }

    private fun processImpl(target: ChangeAwareList<T>, changes: ChangeSet<T>): ChangeSet<T> {
        val refreshes = ArrayList<T>(changes.refreshes)

        changes.forEach { change ->
            val current = change.item.current

            when (change.reason) {
                ListChangeReason.Add ->
                    insert(target, current)

                ListChangeReason.AddRange -> {
                    val ordered = change.range.sortedWith(_comparator).toList()
                    if (target.size == 0)
                        target.addAll(ordered)
                    else
                        ordered.forEach { insert(target, it) }
                }

                ListChangeReason.Remove ->
                    remove(target, current)

                ListChangeReason.Refresh -> {
                    //add to refresh list so position can be calculated
                    refreshes.add(change.item.current)

                    //add to current list so downstream operators can receive a refresh
                    //notification, so get the latest index and pass the index up the chain
                    val indexed = target
                        .indexOfOptional(change.item.current)
                        .valueOrThrow { SortException("Cannot find index of -> ${change.item.current}. Expected to be in the list.") }

                    target.refresh(indexed.item, indexed.index)
                }

                ListChangeReason.Replace -> {
                    //TODO: check whether an item should stay in the same position
                    //i.e. update and move
                    remove(target, change.item.previous.value)
                    insert(target, current)
                }

                ListChangeReason.RemoveRange ->
                    target.removeMany(change.range)

                ListChangeReason.Clear ->
                    target.clear()

                else -> Functions.EMPTY_ACTION
            }
        }

        //Now deal with refreshes [can be expensive]
        refreshes.forEach {
            val old = target.indexOf(it)
            if (old == -1) return@forEach

            var newPosition = getInsertPositionLinear(target, it)

            if (old < newPosition) newPosition--

            if (old == newPosition) return@forEach

            target.move(old, newPosition)
        }

        return target.captureChanges()
    }

    private fun remove(target: ChangeAwareList<T>, item: T) {
        val idx = getCurrentPosition(target, item)
        target.removeAt(idx)
    }

    private fun getCurrentPosition(target: ChangeAwareList<T>, item: T): Int {
        val idx =
            if (_sortOption == SortOption.BinarySearch) target.binarySearch(item, _comparator) else target.indexOf(item)

        if (idx < 0)
            throw SortException("Cannot find item: -> $item.")

        return idx
    }

    private fun insert(target: ChangeAwareList<T>, item: T) {
        val idx = getInsertPosition(target, item)
        target.add(idx, item)
    }

    private fun getInsertPosition(target: ChangeAwareList<T>, item: T): Int =
        if (_sortOption == SortOption.BinarySearch)
            getInsertPositionBinary(target, item)
        else
            getInsertPositionLinear(target, item)

    private fun getInsertPositionBinary(target: ChangeAwareList<T>, item: T): Int {
        val idx = target.binarySearch(item, _comparator)
        val insertIdx = idx.inv()

        if (insertIdx < 0)
            throw SortException("Binary search has been specified, yet the sort does not yield uniqueness")

        return insertIdx
    }

    private fun getInsertPositionLinear(target: ChangeAwareList<T>, item: T): Int {
        (0 until target.size).forEach { i ->
            if (_comparator.compare(item, target[i]) < 0)
                return i
        }

        return target.size
    }
}
