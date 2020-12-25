package xyz.magentaize.dynamicdata.cache.internal

import xyz.magentaize.dynamicdata.cache.ChangeSet

internal interface Filterable<K, V> {
    val filter: (V) -> Boolean

    fun refresh(items: Iterable<Map.Entry<K, V>>): ChangeSet<K, V>

    fun update(updates: ChangeSet<K, V>): ChangeSet<K, V>
}