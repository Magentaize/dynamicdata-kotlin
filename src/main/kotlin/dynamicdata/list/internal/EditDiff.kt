package dynamicdata.list.internal

import dynamicdata.list.ISourceList

internal class EditDiff<T>(
    private val _source: ISourceList<T>,
    private val _equalityComparer: (T, T) -> Boolean
) {
    fun edit(items: Iterable<T>) =
        _source.edit { innerList ->
            val originalItems = innerList.toList()
            val newItems = items.toList()
            val removes = originalItems.filter { e -> !newItems.any { _equalityComparer(e, it) } }
            val adds = newItems.filter { e -> !originalItems.any { _equalityComparer(e, it) } }

            innerList.removeAll(removes)
            innerList.addAll(adds)
        }
}
