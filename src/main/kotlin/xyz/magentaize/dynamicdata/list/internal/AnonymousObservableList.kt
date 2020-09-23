package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.list.ChangeSet
import xyz.magentaize.dynamicdata.list.ObservableList
import xyz.magentaize.dynamicdata.list.EditableObservableList
import xyz.magentaize.dynamicdata.list.SourceList
import io.reactivex.rxjava3.core.Observable

internal class AnonymousObservableList<T>(private val sourceList: EditableObservableList<T>) : ObservableList<T> by sourceList {
    constructor(source: Observable<ChangeSet<T>>) : this(SourceList(source))

    override val items: Iterable<T>
        get() = sourceList.items
}
