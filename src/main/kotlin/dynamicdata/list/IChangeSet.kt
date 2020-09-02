package dynamicdata.list

import dynamicdata.IChangeSet

interface IChangeSet<T> : Iterable<Change<T>>, IChangeSet {
    val size: Int
    val replaced: Int
    val totalChanges: Int
}
