package xyz.magentaize.dynamicdata.cache

import xyz.magentaize.dynamicdata.ChangeSet

interface ChangeSet<K, V> : ChangeSet, Iterable<Change<K, V>> {
    val size: Int
    val updates: Int
}
