package dynamicdata.list.internal

import dynamicdata.list.Group
import dynamicdata.list.IExtendedList
import dynamicdata.list.IObservableList
import dynamicdata.list.SourceList

internal class AnonymousMutableGroup<T, K>(override val key: K) : Group<T, K> {
    private val _source = SourceList<T>()

    override val list: IObservableList<T> = _source

    fun edit(action: (IExtendedList<T>) -> Unit) = _source.edit(action)

    override fun toString(): String =
        "Group of $key (${_source.size} records"
}
