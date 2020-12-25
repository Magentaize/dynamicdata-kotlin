package xyz.magentaize.dynamicdata.cache

import io.reactivex.rxjava3.core.Observable

interface ConnectableCache<K, V> {
    fun connect(predicate: ((V) -> Boolean)? = null): Observable<ChangeSet<K, V>>
    fun preview(predicate: ((V) -> Boolean)? = null): Observable<ChangeSet<K, V>>
    fun watch(key: K): Observable<Change<K, V>>
    val sizeChanged: Observable<Int>
}