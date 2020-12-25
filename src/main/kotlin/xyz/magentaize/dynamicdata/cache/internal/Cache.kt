package xyz.magentaize.dynamicdata.cache.internal

import xyz.magentaize.dynamicdata.cache.ChangeReason
import xyz.magentaize.dynamicdata.cache.AnonymousChangeSet
import xyz.magentaize.dynamicdata.cache.ChangeSet
import xyz.magentaize.dynamicdata.cache.ICache
import xyz.magentaize.dynamicdata.kernel.Optional
import xyz.magentaize.dynamicdata.kernel.lookup

internal class Cache<K, V> : ICache<K, V> {
    private val data: MutableMap<K, V>

    constructor(capacity: Int = -1) {
        data = if (capacity > 1) LinkedHashMap(capacity) else LinkedHashMap()
    }

    constructor(data: Map<K, V>) {
        this.data = data.toMutableMap()
    }

    val size: Int
        get() = data.size

    override val keyValues: Map<K, V>
        get() = data

    override val items: Iterable<V>
        get() = data.values

    override val keys: Iterable<K>
        get() = data.keys

    override fun clone(changes: ChangeSet<K, V>) =
        changes.forEach {
            when (it.reason) {
                ChangeReason.Add, ChangeReason.Update -> data[it.key] = it.current
                ChangeReason.Remove -> data.remove(it.key)
                else -> {
                }
            }
        }

    override fun addOrUpdate(item: V, key: K) {
        data[key] = item
    }

    override fun remove(key: K) {
        data.remove(key)
    }

    override fun remove(keys: Iterable<K>) =
        keys.forEach{remove(it)}

    override fun clear() =
        data.clear()

    override fun refresh() {
        TODO("Not yet implemented")
    }

    override fun refresh(keys: Iterable<K>) {
        TODO("Not yet implemented")
    }

    override fun refresh(key: K) {
        TODO("Not yet implemented")
    }

    override fun lookup(key: K): Optional<V> =
        data.lookup(key)
}
