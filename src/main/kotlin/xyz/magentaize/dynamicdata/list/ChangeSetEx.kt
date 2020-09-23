package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.kernel.convert
import xyz.magentaize.dynamicdata.list.linq.ItemChangeEnumerator
import xyz.magentaize.dynamicdata.list.linq.UnifiedChangeEnumerator
import xyz.magentaize.dynamicdata.list.linq.WithoutIndexEnumerator

fun <T> Iterable<Change<T>>.yieldWithoutIndex(): Iterable<Change<T>> =
    WithoutIndexEnumerator(this)

internal fun <T> ChangeSet<T>.unified() =
    UnifiedChangeEnumerator(this)

fun <T> ChangeSet<T>.flatten(): Iterable<ItemChange<T>> =
    ItemChangeEnumerator(this)

fun ListChangeReason.getChangeType() =
    when (this) {
            ListChangeReason.Add,
            ListChangeReason.Refresh,
            ListChangeReason.Replace,
            ListChangeReason.Moved,
            ListChangeReason.Remove
         -> ChangeType.Item

            ListChangeReason.AddRange,
            ListChangeReason.RemoveRange,
            ListChangeReason.Clear
         -> ChangeType.Range
    }

fun <T, R> ChangeSet<T>.transform(transformer: (T) -> R): ChangeSet<R> {
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

    return AnonymousChangeSet(changes)
}
