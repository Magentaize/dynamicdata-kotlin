package dynamicdata.list

import dynamicdata.IChangeSet

interface IChangeSet<T> : Iterable<Change<T>>, IChangeSet {
    val replaced: Int
    val totalChanges: Int
}
