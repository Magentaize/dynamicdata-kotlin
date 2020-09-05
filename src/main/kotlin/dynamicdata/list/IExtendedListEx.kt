package dynamicdata.list

import dynamicdata.kernel.duplicates
import dynamicdata.kernel.indexOfMany
import java.lang.IllegalArgumentException

fun <T> MutableList<T>.clone(changes: IChangeSet<T>) =
    clone(changes as Iterable<Change<T>>)

fun <T> MutableList<T>.clone(changes: Iterable<Change<T>>) =
    changes.forEach { clone(it) }

private fun <T> MutableList<T>.clone(item: Change<T>) {
    val changeAware = this as? ChangeAwareList<T>

    when (item.reason) {
        ListChangeReason.Add -> {
            val change = item.item
            if (change.currentIndex >= 0)
                this.add(change.currentIndex, change.current)
            else
                this.add(change.current)
        }
        ListChangeReason.AddRange -> this.addOrInsertRange(item.range, item.range.index)
        ListChangeReason.Clear -> this.clearOrRemoveMany(item)
        ListChangeReason.Replace -> {
            val change = item.item
            if (change.currentIndex >= 0 && change.currentIndex == change.previousIndex) {
                this[change.currentIndex] = change.current
            } else {
                if (change.previousIndex == -1) {
                    this.remove(change.previous)
                } else {
                    //is this best? or replace + move?
                    this.removeAt(change.previousIndex)
                }

                if (change.currentIndex == -1) {
                    this.add(change.current)
                } else {
                    this.add(change.currentIndex, change.current)
                }
            }
        }
        ListChangeReason.Refresh ->
            if (changeAware != null) {
                changeAware.refreshAt(item.item.currentIndex)
            } else {
                this.removeAt(item.item.currentIndex)
                this.add(item.item.currentIndex, item.item.current)
            }
        ListChangeReason.Remove -> {
            val change = item.item
            if (change.currentIndex >= 0) {
                this.removeAt(change.currentIndex)
            } else {
                this.remove(change.current)
            }
        }
        ListChangeReason.RemoveRange ->
            //ignore this case because WhereReasonsAre removes the index [in which case call RemoveMany]
            //if (item.Range.Index < 0)
            //    throw new UnspecifiedIndexException("ListChangeReason.RemoveRange should not have an index specified index");
            if (item.range.index >= 0 && (this is IExtendedList<*>)) {
                this.removeAll(item.range.index, item.range.size)
            } else {
                this.removeAll(item.range)
            }
        ListChangeReason.Moved -> {
            val change = item.item
            if (change.currentIndex < 0)
                throw UnspecifiedIndexException("Cannot move as an index was not specified")

            if (this is IExtendedList<*>) {
                this.move(change.previousIndex, change.currentIndex)
            } else {
                //check this works whatever the index is
                this.removeAt(change.previousIndex)
                this.add(change.currentIndex, change.current)
            }
        }
    }
}

fun <T> MutableList<T>.addOrInsertRange(items: Iterable<T>, index: Int) {
    if (index >= 0) {
        this.addAll(index, items.toList())
    } else {
        this.addAll(items.toList())
    }
}

fun <T> MutableList<T>.clearOrRemoveMany(change: Change<T>) {
    if (this.size == change.range.size)
        this.clear()
    else
        this.removeMany(change.range)
}

fun <T> MutableList<T>.removeMany(itemsToRemove: Iterable<T>) {
    val toRemoveList = itemsToRemove.toList()

    // match all indicies and and remove in reverse as it is more efficient
    val toRemove = this.indexOfMany(toRemoveList)
        .sortedByDescending { it.index }
        .toList()

    //if there are duplicates, it could be that an item exists in the
    //source collection more than once - in that case the fast remove
    //would remove each instance
    val hasDuplicates = toRemove.duplicates { it.value }.any()

    if (hasDuplicates)
    //Slow remove but safe
        toRemoveList.forEach { this.remove(it) }
    else
    //Fast remove because we know the index of all and we remove in order
        toRemove.forEach { this.removeAt(it.index) }
}

fun <T> MutableList<T>.replace(original: T, replaceWith: T) {
    val index = this.indexOf(original)
    if (index == -1)
        throw IllegalArgumentException("Cannot find index of original item. Either it does not exist in the list or the hashcode has mutated")
    else
        this[index] = replaceWith
}

fun <T> MutableList<T>.replaceOrAdd(original: T, replaceWith: T) {
    val index = this.indexOf(original)
    if (index == -1)
        this.add(replaceWith)
    else
        this[index] = replaceWith
}
