package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.cache.monitorStatus
import xyz.magentaize.dynamicdata.kernel.ConnectionStatus
import xyz.magentaize.dynamicdata.list.AnonymousChangeSet
import xyz.magentaize.dynamicdata.list.ChangeSet
import xyz.magentaize.dynamicdata.list.notEmpty
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
