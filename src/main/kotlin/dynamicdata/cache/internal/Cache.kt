package dynamicdata.cache.internal

import dynamicdata.cache.ChangeReason
import dynamicdata.cache.ChangeSet
import dynamicdata.cache.ICache

internal class Cache<TObject, TKey> : ICache<TObject, TKey> {
    private val data: MutableMap<TKey, TObject>

    constructor(capacity: Int = -1) {
        data = if (capacity > 1) LinkedHashMap(capacity) else LinkedHashMap()
    }

    constructor(data: Map<TKey, TObject>) {
        this.data = data.toMutableMap()
    }

    val size: Int
        get() = data.size

    override val keyValues: Map<TKey, TObject>
        get() = data

    override val items: Iterable<TObject>
        get() = data.values

    override val keys: Iterable<TKey>
        get() = data.keys

    override fun clone(changes: ChangeSet<TObject, TKey>) =
        changes.forEach {
            when (it.reason) {
                ChangeReason.Add, ChangeReason.Update -> data[it.key] = it.current
                ChangeReason.Remove -> data.remove(it.key)
                else -> {
                }
            }
        }

    override fun addOrUpdate(item: TObject, key: TKey) {
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

    override fun lookup(key: TKey): TObject? =
        data[key]
}
