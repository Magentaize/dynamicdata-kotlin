package dynamicdata.list.internal

import dynamicdata.list.ChangeSet
import dynamicdata.list.ObservableList
import dynamicdata.list.EditableObservableList
import dynamicdata.list.SourceList
import io.reactivex.rxjava3.core.Observable

internal class AnonymousObservableList<T>(private val sourceList: EditableObservableList<T>) : ObservableList<T> by sourceList {
    constructor(source: Observable<ChangeSet<T>>) : this(SourceList(source))

    override val items: Iterable<T>
        get() = sourceList.items
}
