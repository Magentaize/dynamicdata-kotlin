package xyz.magentaize.dynamicdata.cache.internal

import xyz.magentaize.dynamicdata.cache.ChangeReason
import xyz.magentaize.dynamicdata.cache.AnonymousChangeSet
import xyz.magentaize.dynamicdata.cache.ChangeSet
import xyz.magentaize.dynamicdata.cache.ICache
import xyz.magentaize.dynamicdata.kernel.Optional
import xyz.magentaize.dynamicdata.kernel.lookup

internal class Cache<T, TKey> : ICache<T, TKey> {
    private val data: MutableMap<TKey, T>

    constructor(capacity: Int = -1) {
        data = if (capacity > 1) LinkedHashMap(capacity) else LinkedHashMap()
    }

    constructor(data: Map<TKey, T>) {
        this.data = data.toMutableMap()
    }

    val size: Int
        get() = data.size

    override val keyValues: Map<TKey, T>
        get() = data

    override val items: Iterable<T>
        get() = data.values

    override val keys: Iterable<TKey>
        get() = data.keys

    override fun clone(changes: ChangeSet<T, TKey>) =
        changes.forEach {
            when (it.reason) {
                ChangeReason.Add, ChangeReason.Update -> data[it.key] = it.current
                ChangeReason.Remove -> data.remove(it.key)
                else -> {
                }
            }
        }

    override fun addOrUpdate(item: T, key: TKey) {
        data[key] = item
    }

    override fun remove(key: TKey) {
        data.remove(key)
    }

    override fun remove(keys: Iterable<TKey>) =
        keys.forEach{remove(it)}

    override fun clear() =
        data.clear()

    override fun refresh() {
        TODO("Not yet implemented")
    }

    override fun refresh(keys: Iterable<TKey>) {
        TODO("Not yet implemented")
    }

    override fun refresh(key: TKey) {
        TODO("Not yet implemented")
    }

    override fun lookup(key: TKey): Optional<T> =
        data.lookup(key)
}
