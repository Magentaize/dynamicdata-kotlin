package dynamicdata.list.internal

import dynamicdata.cache.monitorStatus
import dynamicdata.kernel.ConnectionStatus
import dynamicdata.list.ChangeSet
import dynamicdata.list.IChangeSet
import dynamicdata.list.notEmpty
import io.reactivex.rxjava3.core.Observable

internal class DeferUntilLoaded<T>(
    private val _source: Observable<IChangeSet<T>>
) {
    fun run(): Observable<IChangeSet<T>> =
        _source.monitorStatus()
            .filter { it == ConnectionStatus.Loaded }
            .take(1)
            .map<IChangeSet<T>> { ChangeSet<T>() }
            .concatWith(_source)
            .notEmpty()
}