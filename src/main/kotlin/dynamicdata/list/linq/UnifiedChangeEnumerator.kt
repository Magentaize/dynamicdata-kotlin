package dynamicdata.list.linq

import dynamicdata.list.ChangeType
import dynamicdata.list.ChangeSet
import dynamicdata.list.ListChangeReason
import dynamicdata.list.internal.UnifiedChange

internal class UnifiedChangeEnumerator<T>(private val changeSet: ChangeSet<T>) : Iterable<UnifiedChange<T>> {
    override fun iterator(): Iterator<UnifiedChange<T>> =
        iterator {
            changeSet.forEach { change ->
                if (change.type == ChangeType.Item)
                    yield(UnifiedChange(change.reason, change.item.current, change.item.previous))
                else
                    when (change.reason) {
                        ListChangeReason.AddRange ->
                            yieldAll(change.range.map { UnifiedChange(ListChangeReason.Add, it) })
                        ListChangeReason.RemoveRange ->
                            yieldAll(change.range.map { UnifiedChange(ListChangeReason.Remove, it) })
                        ListChangeReason.Clear ->
                            yieldAll(change.range.map { UnifiedChange(ListChangeReason.Clear, it) })
                        else -> {
                        }
                    }
            }
        }
}
