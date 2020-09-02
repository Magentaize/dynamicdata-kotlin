package dynamicdata.cache

import dynamicdata.IChangeSet

interface IChangeSet<TObject, TKey> : IChangeSet, Iterable<Change<TObject, TKey>> {
    val size: Int
    val updates: Int
}
