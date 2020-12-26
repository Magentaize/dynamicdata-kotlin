package xyz.magentaize.dynamicdata.cache.internal

import xyz.magentaize.dynamicdata.cache.ChangeReason
import xyz.magentaize.dynamicdata.cache.ChangeSet
import xyz.magentaize.dynamicdata.cache.ICache
import xyz.magentaize.dynamicdata.kernel.Optional
import xyz.magentaize.dynamicdata.kernel.lookup

internal class Cache<K, V>(private val _data: MutableMap<K, V>) : ICache<K, V> {

    constructor(capacity: Int = -1) : this(
        if (capacity > 1) LinkedHashMap(capacity) else LinkedHashMap()
    )

    val size: Int
        get() = _data.size

    override val keyValues: Map<K, V>
        get() = _data

    override val items: Iterable<V>
        get() = _data.values

    override val keys: Iterable<K>
        get() = _data.keys

    override fun clone(changes: ChangeSet<K, V>) =
        changes.forEach {
            when (it.reason) {
                ChangeReason.Add, ChangeReason.Update -> _data[it.key] = it.current
                ChangeReason.Remove -> _data.remove(it.key)
                else -> {
                }
            }
        }

    override fun addOrUpdate(item: V, key: K) {
        _data[key] = item
    }

    override fun remove(key: K) {
        _data.remove(key)
    }

    override fun remove(keys: Iterable<K>) =
        keys.forEach { remove(it) }

    override fun clear() =
        _data.clear()

    override fun refresh() {
    }

    override fun refresh(keys: Iterable<K>) {
    }

    override fun refresh(key: K) {
    }

    override fun lookup(key: K): Optional<V> =
        _data.lookup(key)
}
