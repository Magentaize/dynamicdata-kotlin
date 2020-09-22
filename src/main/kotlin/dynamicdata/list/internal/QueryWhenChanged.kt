package dynamicdata.list.internal

import dynamicdata.list.ChangeSet
import dynamicdata.list.clone
import io.reactivex.rxjava3.core.Observable

internal class QueryWhenChanged<T>(
    private val _source: Observable<ChangeSet<T>>
) {
    fun run(): Observable<List<T>> =
        _source.scan(mutableListOf()) { list: MutableList<T>, changes ->
            list.clone(changes)
            list
        }
            .skip(1)
            .map { it.toList() }

}
