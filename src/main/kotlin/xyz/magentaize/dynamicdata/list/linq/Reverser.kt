package xyz.magentaize.dynamicdata.list.linq

import xyz.magentaize.dynamicdata.list.Change
import xyz.magentaize.dynamicdata.list.ChangeSet
import xyz.magentaize.dynamicdata.list.ListChangeReason
import io.reactivex.rxjava3.internal.functions.Functions

internal class Reverser<T> {
    private var _length = 0

    fun reverse(changes: ChangeSet<T>): Iterator<Change<T>> =
        iterator {
            changes.forEach { change ->
                when (change.reason) {
                    ListChangeReason.Add -> {
                        yield(Change(ListChangeReason.Add, change.item.current, _length - change.item.currentIndex))
                        _length++
                    }

                    ListChangeReason.AddRange -> {
                        val offset = if (change.range.index == -1) 0 else _length - change.range.index
                        yield(Change(ListChangeReason.AddRange, change.range.reversed(), offset))
                        _length += change.range.size
                    }

                    ListChangeReason.Replace -> {
                        val new = _length - change.item.currentIndex - 1
                        yield(Change(ListChangeReason.Replace, change.item.current, change.item.previous, new, new))
                    }

                    ListChangeReason.Remove -> {
                        yield(
                            Change(
                                ListChangeReason.Remove,
                                change.item.current,
                                _length - change.item.currentIndex - 1
                            )
                        )
                        _length--
                    }

                    ListChangeReason.RemoveRange -> {
                        val offset = _length - change.range.index - change.range.size
                        yield(Change(ListChangeReason.RemoveRange, change.range.reversed(), offset))
                        _length -= change.range.size
                    }

                    ListChangeReason.Moved -> {
                        val curr = _length - change.item.currentIndex - 1
                        val prev = _length - change.item.previousIndex - 1
                        yield(Change(change.item.current, curr, prev))
                    }

                    ListChangeReason.Clear -> {
                        yield(Change(ListChangeReason.Clear, change.range.reversed()))
                        _length = 0
                    }

                    else -> Functions.EMPTY_ACTION
                }
            }
        }
}
