package dynamicdata.list.internal

import dynamicdata.list.MutableGroup
import dynamicdata.list.IExtendedList
import dynamicdata.list.ObservableList
import dynamicdata.list.SourceList

internal class AnonymousMutableGroup<T, K>(override val key: K) : MutableGroup<T, K> {
    private val _source = SourceList<T>()

    override val list: ObservableList<T> = _source

    fun edit(action: (IExtendedList<T>) -> Unit) = _source.edit(action)

    override fun toString(): String =
        "Group of $key (${_source.size} records"
}
