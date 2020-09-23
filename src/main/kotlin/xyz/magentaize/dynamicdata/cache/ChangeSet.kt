package xyz.magentaize.dynamicdata.cache

import xyz.magentaize.dynamicdata.ChangeSet

interface ChangeSet<TObject, TKey> : ChangeSet, Iterable<Change<TObject, TKey>> {
    val size: Int
    val updates: Int
}
