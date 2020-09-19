package dynamicdata.list.internal

import dynamicdata.list.IChangeSet
import dynamicdata.list.ObservableList
import dynamicdata.list.ISourceList
import dynamicdata.list.SourceList
import io.reactivex.rxjava3.core.Observable

internal class AnonymousObservableList<T>(private val sourceList: ISourceList<T>) : ObservableList<T> by sourceList {
    constructor(source: Observable<IChangeSet<T>>) : this(SourceList(source))

    override val items: Iterable<T>
        get() = sourceList.items
}
