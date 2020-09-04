package dynamicdata.list

import dynamicdata.kernel.convert
import dynamicdata.list.linq.ItemChangeEnumerator
import dynamicdata.list.linq.UnifiedChangeEnumerator

internal fun <T> IChangeSet<T>.unified() =
    UnifiedChangeEnumerator(this)

fun <T> IChangeSet<T>.flatten(): Iterable<ItemChange<T>> =
    ItemChangeEnumerator(this)

fun ListChangeReason.getChangeType() =
    when (this) {
        in setOf(
            ListChangeReason.Add,
            ListChangeReason.Refresh,
            ListChangeReason.Replace,
            ListChangeReason.Moved,
            ListChangeReason.Remove
        ) -> ChangeType.Item
        in setOf(
            ListChangeReason.AddRange,
            ListChangeReason.RemoveRange,
            ListChangeReason.Clear
        ) -> ChangeType.Range
        else -> throw IllegalArgumentException()
    }

fun <T, R> IChangeSet<T>.transform(transformer: (T) -> R): IChangeSet<R> {
    val changes = this.map {
        if (it.type == ChangeType.Item)
            Change(
                it.reason,
                transformer(it.item.current),
                it.item.previous.convert(transformer),
                it.item.currentIndex,
                it.item.previousIndex
            )
        else
            Change(it.reason, it.range.map(transformer), it.range.index)
    }

    return ChangeSet(changes)
}
