package xyz.magentaize.dynamicdata.cache

import io.reactivex.rxjava3.disposables.Disposable
import xyz.magentaize.dynamicdata.kernel.Optional

interface ObservableCache<K, V> : ConnectableCache<K, V>, Disposable {
    val size: Int
    val items: Iterable<V>
    val keys: Iterable<K>
    val keyValues: Iterable<Map.Entry<K, V>>
    fun lookup(key: K): Optional<V>
}
