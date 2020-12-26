package xyz.magentaize.dynamicdata.cache

import io.reactivex.rxjava3.core.Observable
import xyz.magentaize.dynamicdata.cache.internal.filterChanges

internal class StaticFilter<K, V>(
    private val _source: Observable<ChangeSet<K, V>>,
    private val _filter: (V) -> Boolean
) {
    fun run(): Observable<ChangeSet<K, V>> =
        _source.scan(ChangeAwareCache.empty<K, V>()) { state, changes ->
            val cache = state ?: ChangeAwareCache(changes.size)
            cache.filterChanges(changes, _filter)

            return@scan cache
        }
            .skip(1)
            .filter { it != ChangeAwareCache.empty<K, V>() }
            .map { it.captureChanges() }
            .notEmpty()
}