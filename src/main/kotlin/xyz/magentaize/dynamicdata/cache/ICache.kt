package xyz.magentaize.dynamicdata.cache

interface ICache<TObject, TKey> : IQuery<TObject, TKey> {
    fun clone(changes: ChangeSet<TObject, TKey>)
    fun addOrUpdate(item: TObject, key: TKey)
    fun remove(key: TKey)
    fun remove(keys: Iterable<TKey>)
    fun clear()
    fun refresh()
    fun refresh(keys: Iterable<TKey>)
    fun refresh(key: TKey)
}
