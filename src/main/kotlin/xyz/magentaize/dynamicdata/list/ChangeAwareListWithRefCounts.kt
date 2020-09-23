package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.list.internal.ReferenceCountTracker

internal class ChangeAwareListWithRefCounts<T> : ChangeAwareList<T>() {
    private val _tracker = ReferenceCountTracker<T>()

    override fun add(index: Int, item: T) {
        _tracker.add(item)
        super.add(index, item)
    }

    override fun onInsertItems(startIndex: Int, items: Collection<T>) {
        items.forEach { _tracker.add(it) }
    }

    override fun removeItem(index: Int, item: T) {
        _tracker.remove(item)
        super.removeItem(index, item)
    }

    override fun onRemoveItems(startIndex: Int, items: Collection<T>) {
        items.forEach { _tracker.remove(it) }
    }

    override fun onSetItem(index: Int, newItem: T, oldItem: T) {
        _tracker.remove(oldItem)
        _tracker.add(newItem)
        super.onSetItem(index, newItem, oldItem)
    }

    override fun contains(element: T): Boolean =
        _tracker.contains(element)

    override fun clear() {
        _tracker.clear()
        super.clear()
    }
}
