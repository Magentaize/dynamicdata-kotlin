package xyz.magentaize.dynamicdata.cache.internal

import xyz.magentaize.dynamicdata.cache.*
import xyz.magentaize.dynamicdata.kernel.Stub

internal class CacheUpdater<K, V>(
    private val _cache: ICache<K, V>,
    private val _keySelector: (V) -> K = { throw IllegalStateException() }
) : ISourceUpdater<K, V>, IQuery<K, V> by _cache {

    constructor(data: MutableMap<K, V>, keySelector: (V) -> K = Stub.emptyMapper())
            : this(Cache(data), keySelector)

    override fun load(items: Iterable<V>) {
        clear()
        addOrUpdate(items)
    }

    override fun addOrUpdate(item: V) =
        _cache.addOrUpdate(item, _keySelector(item))

    override fun addOrUpdate(items: Iterable<V>) =
        items.forEach { _cache.addOrUpdate(it, _keySelector(it)) }

    override fun addOrUpdate(item: Pair<K, V>) =
        TODO()

    override fun addOrUpdate(item: V, key: K) =
        _cache.addOrUpdate(item, key)

    override fun refresh() =
        _cache.refresh()

    override fun refresh(keys: Iterable<K>) =
        keys.forEach { refresh(it) }

    override fun refresh(key: K) =
        _cache.refresh(key)

    override fun removeItem(items: Iterable<V>) =
        items.forEach { removeItem(it) }

    override fun remove(keys: Iterable<K>) =
        _cache.remove(keys)

    override fun removeItem(item: V) =
        _cache.remove(_keySelector(item))

    override fun remove(key: K) =
        _cache.remove(key)

    override fun removeKvp(items: Iterable<Pair<K, V>>) {
        TODO("Not yet implemented")
    }

    override fun removePair(item: Pair<K, V>) {
        TODO("Not yet implemented")
    }

    override fun removeKvp(item: Pair<K, V>) {
        TODO("Not yet implemented")
    }

    override fun clone(changes: ChangeSet<K, V>) {
        _cache.clone(changes)
    }

    override fun clear() =
        _cache.clear()

    override fun getKey(item: V): K =
        _keySelector(item)

    override fun getKeyValues(items: Iterable<V>): Map<K, V> =
        items.associateBy(_keySelector)
}
