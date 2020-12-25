package xyz.magentaize.dynamicdata.cache

import io.reactivex.rxjava3.core.Observable
import xyz.magentaize.dynamicdata.kernel.Optional

class SourceCache<K, V>(
    override val keySelector: (V) -> K
) : EditableSourceCache<K, V> {
    private val _innerCache: AnonymousObservableCache<K, V> = AnonymousObservableCache(keySelector)
    private var _isDisposed = false

    override fun connect(predicate: ((V) -> Boolean)?): Observable<ChangeSet<K, V>> =
        _innerCache.connect(predicate)

    override fun preview(predicate: ((V) -> Boolean)?): Observable<ChangeSet<K, V>> =
        _innerCache.preview(predicate)

    override fun watch(key: K): Observable<Change<K, V>> =
        _innerCache.watch(key)

    override fun dispose() {
        _innerCache.dispose()
        _isDisposed = true
    }

    override fun isDisposed(): Boolean =
        _isDisposed

    override fun edit(updateAction: (ISourceUpdater<K, V>) -> Unit) =
        _innerCache.updateFromSource(updateAction)

    override fun lookup(key: K): Optional<V> =
        _innerCache.lookup(key)

    override val sizeChanged: Observable<Int>
        get() = _innerCache.sizeChanged

    override val size: Int
        get() = _innerCache.size

    override val items: Iterable<V>
        get() = _innerCache.items

    override val keys: Iterable<K>
        get() = _innerCache.keys

    override val keyValues: Iterable<Map.Entry<K, V>>
        get() = _innerCache.keyValues
}