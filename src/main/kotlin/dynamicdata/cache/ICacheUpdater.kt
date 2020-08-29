package dynamicdata.cache

interface ICacheUpdater<TObject, TKey> : IQuery<TObject, TKey> {
    //fun addOrUpdate(keyValuePairs: Iterable<Pair<TKey, TObject>>)
    fun addOrUpdate(item: Pair<TObject, TKey>)
    fun addOrUpdate(item: TObject, key: TKey)
    fun refresh()
    fun refresh(keys: Iterable<TKey>)
    fun refresh(key: TKey)
    fun removeItem(keys: Iterable<TObject>)

    /*Overload of remove due to ambiguous method when TObject and TKey are of the same type*/
    fun remove(keys: Iterable<TKey>)
    fun removeItem(key: TObject)
    fun remove(key: TKey)
    fun removeKvp(items: Iterable<Pair<TKey, TObject>>)
    fun removePair(item: Pair<TKey, TObject>)
    fun removeKvp(item: Pair<TKey, TObject>)
    fun clone(changes: ChangeSet<TObject, TKey>)
    fun clear()
    fun getKey(item: TObject): TKey
    fun getKeyValues(items: Iterable<TObject>): Map<TKey, TObject>
}
