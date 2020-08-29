package dynamicdata.cache

import java.util.*

interface IQuery<TObject, TKey> {
    fun lookup(key: TKey): TObject?

    val keys: Iterable<TKey>

    val items: Iterable<TObject>

    val keyValues: Map<TKey, TObject>

    //val count: Int
}
