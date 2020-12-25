package xyz.magentaize.dynamicdata.cache

interface ICache<K, V> : IQuery<K, V> {
    fun clone(changes: ChangeSet<K, V>)
    fun addOrUpdate(item: V, key: K)
    fun remove(key: K)
    fun remove(keys: Iterable<K>)
    fun clear()
    fun refresh()
    fun refresh(keys: Iterable<K>)
    fun refresh(key: K)
}
