package xyz.magentaize.dynamicdata.cache

interface ICacheUpdater<K, V> : IQuery<K, V> {
    //fun addOrUpdate(keyValuePairs: Iterable<Pair<TKey, TObject>>)
    fun addOrUpdate(item: Pair<K, V>)
    fun addOrUpdate(item: V, key: K)
    fun refresh()
    fun refresh(keys: Iterable<K>)
    fun refresh(key: K)
    fun removeItem(keys: Iterable<V>)

    /*Overload of remove due to ambiguous method when TObject and TKey are of the same type*/
    fun remove(keys: Iterable<K>)
    fun removeItem(item: V)
    fun remove(key: K)
    fun removeKvp(items: Iterable<Pair<K, V>>)
    fun removePair(item: Pair<K, V>)
    fun removeKvp(item: Pair<K, V>)
    fun clone(changes: ChangeSet<K, V>)
    fun clear()
    fun getKey(item: V): K
    fun getKeyValues(items: Iterable<V>): Map<K, V>
}
