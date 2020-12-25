package xyz.magentaize.dynamicdata.cache

import xyz.magentaize.dynamicdata.kernel.Optional

interface IQuery<K, V> {
    fun lookup(key: K): Optional<V>

    val keys: Iterable<K>

    val items: Iterable<V>

    val keyValues: Map<K, V>

    //val count: Int
}
