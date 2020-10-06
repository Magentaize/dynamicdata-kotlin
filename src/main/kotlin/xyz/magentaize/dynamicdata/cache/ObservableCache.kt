package xyz.magentaize.dynamicdata.cache

import io.reactivex.rxjava3.core.Observable

interface ConnectableCache<T, K> {
    fun connect(predicate: ((T) -> K)? = null): Observable<ChangeSet<T, K>>
}

interface ObservableCache<T, K> : ConnectableCache<T, K> {

}
