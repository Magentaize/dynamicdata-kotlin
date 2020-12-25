package xyz.magentaize.dynamicdata.cache.internal

import xyz.magentaize.dynamicdata.cache.*

internal class CacheUpdater<K, V>(
    private val cache: ICache<K, V>,
    private val keySelector: (V) -> K = { throw IllegalStateException() }
) : ISourceUpdater<K, V>, IQuery<K, V> by cache {

    constructor(data: Map<K, V>, keySelector: (V) -> K = { throw IllegalStateException() })
            : this(Cache(data), keySelector)

    override fun load(items: Iterable<V>) {
        clear()
        addOrUpdate(items)
    }

    fun addOrUpdate(item: V) =
        cache.addOrUpdate(item, keySelector(item))

    override fun addOrUpdate(items: Iterable<V>) =
        items.forEach { cache.addOrUpdate(it, keySelector(it)) }

    override fun addOrUpdate(item: Pair<K, V>) =
        TODO()

    override fun addOrUpdate(item: V, key: K) =
        cache.addOrUpdate(item, key)

    override fun refresh() =
        cache.refresh()

    override fun refresh(keys: Iterable<K>) =
        keys.forEach { refresh(it) }

    override fun refresh(key: K) {
        TODO("Not yet implemented")
    }

    override fun removeItem(items: Iterable<V>) =
        items.forEach { removeItem(it) }

    override fun remove(keys: Iterable<K>) {
        TODO("Not yet implemented")
    }

    override fun removeItem(item: V) =
        cache.remove(keySelector(item))

    override fun remove(key: K) =
        cache.remove(key)

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
        TODO("Not yet implemented")
    }

    override fun clear() =
        cache.clear()

    override fun getKey(item: V): K =
        keySelector(item)

    override fun getKeyValues(items: Iterable<V>): Map<K, V> =
        items.associateBy(keySelector)
}
