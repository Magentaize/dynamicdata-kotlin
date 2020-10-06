package xyz.magentaize.dynamicdata.cache

import xyz.magentaize.dynamicdata.kernel.Optional

interface IQuery<T, TKey> {
    fun lookup(key: TKey): Optional<T>

    val keys: Iterable<TKey>

    val items: Iterable<T>

    val keyValues: Map<TKey, T>

    //val count: Int
}
