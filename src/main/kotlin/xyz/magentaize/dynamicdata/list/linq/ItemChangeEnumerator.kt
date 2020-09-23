package xyz.magentaize.dynamicdata.list.linq

import xyz.magentaize.dynamicdata.list.ChangeType
import xyz.magentaize.dynamicdata.list.ChangeSet
import xyz.magentaize.dynamicdata.list.ItemChange
import xyz.magentaize.dynamicdata.list.ListChangeReason

internal class ItemChangeEnumerator<T>(private val _changeSet: ChangeSet<T>) : Iterable<ItemChange<T>> {
    override operator fun iterator(): Iterator<ItemChange<T>> {
        var lastKnownIndex = 0
        return iterator {
            _changeSet.forEach { change ->
                if (change.type == ChangeType.Item) {
                    lastKnownIndex = change.item.currentIndex
                    yield(
                        ItemChange(
                            change.reason,
                            change.item.current,
                            change.item.previous,
                            change.item.currentIndex,
                            change.item.previousIndex
                        )
                    )
                } else {
                    var index = if (change.range.index == -1) lastKnownIndex else change.range.index

                    change.range.forEach {
                        when (change.reason) {
                            ListChangeReason.AddRange ->
                                yield(ItemChange(ListChangeReason.Add, it, index))

                            ListChangeReason.RemoveRange, ListChangeReason.Clear ->
                                yield(ItemChange(ListChangeReason.Remove, it, index))
                            else -> {
                            }
                        }

                        index++
                        lastKnownIndex = index
                    }
                }
            }
        }
    }
}
