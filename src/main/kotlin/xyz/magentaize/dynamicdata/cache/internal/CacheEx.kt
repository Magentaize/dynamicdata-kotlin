package xyz.magentaize.dynamicdata.cache.internal

import xyz.magentaize.dynamicdata.cache.*
import xyz.magentaize.dynamicdata.kernel.Stub

internal fun <K, V> ChangeAwareCache<K, V>.getInitialUpdates(filter: (V) -> Boolean = Stub.emptyFilter()): ChangeSet<K, V> {
    val filtered = if (filter == Stub.emptyFilter<V>())
        this.keyValues
    else
        this.keyValues.filter { filter(it.value) }

    return AnonymousChangeSet(filtered.map { Change(ChangeReason.Add, it.key, it.value) })
}