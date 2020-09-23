package xyz.magentaize.dynamicdata.cache.internal

import xyz.magentaize.dynamicdata.cache.AnonymousChangeSet
import xyz.magentaize.dynamicdata.cache.ICache
import xyz.magentaize.dynamicdata.cache.IQuery
import xyz.magentaize.dynamicdata.cache.ISourceUpdater

internal class CacheUpdater<TObject, TKey>(
    private val cache: ICache<TObject, TKey>,
    private val keySelector: (TObject) -> TKey = { throw IllegalStateException() }
) : ISourceUpdater<TObject, TKey>, IQuery<TObject, TKey> by cache {

    constructor(data: Map<TKey, TObject>, keySelector: (TObject) -> TKey = { throw IllegalStateException() })
            : this(Cache(data), keySelector)

    override fun load(items: Iterable<TObject>) {
        clear()
        addOrUpdate(items)
    }

    fun addOrUpdate(item: TObject) =
        cache.addOrUpdate(item, keySelector(item))

    override fun addOrUpdate(items: Iterable<TObject>) =
        items.forEach { cache.addOrUpdate(it, keySelector(it)) }

    override fun addOrUpdate(item: Pair<TObject, TKey>) =
        TODO()

    override fun addOrUpdate(item: TObject, key: TKey) =
        cache.addOrUpdate(item, key)

    override fun refresh() =
        cache.refresh()

    override fun refresh(keys: Iterable<TKey>) =
        keys.forEach { refresh(it) }

    override fun refresh(key: TKey) {
        TODO("Not yet implemented")
    }

    override fun removeItem(items: Iterable<TObject>) =
        items.forEach { removeItem(it) }

    override fun remove(keys: Iterable<TKey>) {
        TODO("Not yet implemented")
    }

    override fun removeItem(item: TObject) =
        cache.remove(keySelector(item))

    override fun remove(key: TKey) =
        cache.remove(key)

    override fun removeKvp(items: Iterable<Pair<TKey, TObject>>) {
        TODO("Not yet implemented")
    }

    override fun removePair(item: Pair<TKey, TObject>) {
        TODO("Not yet implemented")
    }

    override fun removeKvp(item: Pair<TKey, TObject>) {
        TODO("Not yet implemented")
    }

    override fun clone(changes: AnonymousChangeSet<TObject, TKey>) {
        TODO("Not yet implemented")
    }

    override fun clear() =
        cache.clear()

    override fun getKey(item: TObject): TKey =
        keySelector(item)

    override fun getKeyValues(items: Iterable<TObject>): Map<TKey, TObject> =
        items.associateBy(keySelector)
}
