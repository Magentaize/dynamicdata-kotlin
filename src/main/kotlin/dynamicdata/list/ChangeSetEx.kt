package dynamicdata.list

import dynamicdata.list.linq.UnifiedChangeEnumerator

internal fun <T> IChangeSet<T>.unified() =
    UnifiedChangeEnumerator(this)

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
