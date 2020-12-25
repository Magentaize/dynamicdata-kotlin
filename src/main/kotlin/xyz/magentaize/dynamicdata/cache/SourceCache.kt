package xyz.magentaize.dynamicdata.cache

import io.reactivex.rxjava3.core.Observable
import xyz.magentaize.dynamicdata.kernel.Optional

class SourceCache<K, V> : EditableSourceCache<K, V> {
    override fun connect(predicate: ((V) -> Boolean)?): Observable<ChangeSet<K, V>> {
        TODO("Not yet implemented")
    }

    override fun preview(predicate: ((V) -> Boolean)?): Observable<ChangeSet<K, V>> {
        TODO("Not yet implemented")
    }

    override fun watch(key: K): Observable<Change<K, V>> {
        TODO("Not yet implemented")
    }

    override val sizeChanged: Observable<Int>
        get() = TODO("Not yet implemented")

    override fun dispose() {
        TODO("Not yet implemented")
    }

    override fun isDisposed(): Boolean {
        TODO("Not yet implemented")
    }

    override val keySelector: (V) -> K
        get() = TODO("Not yet implemented")

    override fun edit(updateAction: ISourceUpdater<K, V>) {
        TODO("Not yet implemented")
    }

    override val size: Int
        get() = TODO("Not yet implemented")
    override val items: Iterable<V>
        get() = TODO("Not yet implemented")
    override val keys: Iterable<K>
        get() = TODO("Not yet implemented")
    override val keyValues: Iterable<Map.Entry<K, V>>
        get() = TODO("Not yet implemented")

    override fun lookup(key: K): Optional<V> {
        TODO("Not yet implemented")
    }

}