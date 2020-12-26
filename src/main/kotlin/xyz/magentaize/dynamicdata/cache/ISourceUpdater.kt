package xyz.magentaize.dynamicdata.cache

interface ISourceUpdater<K, V> : ICacheUpdater<K, V>{
    fun load(items: Iterable<V>)
    fun addOrUpdate(items: Iterable<V>)
    fun addOrUpdate(item: V)
}
