package dynamicdata.cache

interface ISourceUpdater<TObject, TKey> : ICacheUpdater<TObject, TKey>{
    fun load(items: Iterable<TObject>)
    fun addOrUpdate(items: Iterable<TObject>)
}
