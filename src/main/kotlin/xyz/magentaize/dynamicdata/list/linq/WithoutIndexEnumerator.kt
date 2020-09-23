package xyz.magentaize.dynamicdata.list.linq

import xyz.magentaize.dynamicdata.list.Change
import xyz.magentaize.dynamicdata.list.ChangeType
import xyz.magentaize.dynamicdata.list.ListChangeReason

internal class WithoutIndexEnumerator<T>(private val _changeSet: Iterable<Change<T>>) : Iterable<Change<T>> {
    override fun iterator(): Iterator<Change<T>> =
        iterator {
            _changeSet.forEach {
                if (it.reason == ListChangeReason.Moved) {
                    //exceptional case - makes no sense to remove index from move
                    return@forEach
                }

                if (it.type == ChangeType.Item)
                    yield(Change(it.reason, it.item.current, it.item.previous))
                else
                    yield(Change(it.reason, it.range))
            }
        }
}
