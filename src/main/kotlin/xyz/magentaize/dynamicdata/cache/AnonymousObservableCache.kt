package xyz.magentaize.dynamicdata.cache

import io.reactivex.rxjava3.core.Observable


internal class AnonymousObservableCache<K, V> constructor(private val sourceCache: EditableSourceCache<K, V>) :
    ObservableCache<K, V> by sourceCache {
    constructor(source: Observable<ChangeSet<K, V>>) : this(SourceCache(source))

    override val items: Iterable<V>
        get() = sourceCache.items
}