package dynamicdata.list.internal

import dynamicdata.cache.monitorStatus
import dynamicdata.kernel.ConnectionStatus
import dynamicdata.list.AnonymousChangeSet
import dynamicdata.list.ChangeSet
import dynamicdata.list.notEmpty
import io.reactivex.rxjava3.core.Observable

internal class DeferUntilLoaded<T>(
    private val _source: Observable<ChangeSet<T>>
) {
    fun run(): Observable<ChangeSet<T>> =
        _source.monitorStatus()
            .filter { it == ConnectionStatus.Loaded }
            .take(1)
            .map<ChangeSet<T>> { AnonymousChangeSet<T>() }
            .concatWith(_source)
            .notEmpty()
}
