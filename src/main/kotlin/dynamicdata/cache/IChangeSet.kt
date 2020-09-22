package dynamicdata.cache

import dynamicdata.ChangeSet

interface IChangeSet<TObject, TKey> : ChangeSet, Iterable<Change<TObject, TKey>> {
    val size: Int
    val updates: Int
}
