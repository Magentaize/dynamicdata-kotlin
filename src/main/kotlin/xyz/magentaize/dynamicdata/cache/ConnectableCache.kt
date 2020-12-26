package xyz.magentaize.dynamicdata.cache

import io.reactivex.rxjava3.core.Observable
import xyz.magentaize.dynamicdata.kernel.Stub

interface ConnectableCache<K, V> {
    fun connect(predicate: (V) -> Boolean = Stub.emptyFilter()): Observable<ChangeSet<K, V>>
    fun preview(predicate: (V) -> Boolean = Stub.emptyFilter()): Observable<ChangeSet<K, V>>
    fun watch(key: K): Observable<Change<K, V>>
    val sizeChanged: Observable<Int>
}