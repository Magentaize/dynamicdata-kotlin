package dynamicdata.list

import dynamicdata.ChangeSet

interface ChangeSet<T> : Iterable<Change<T>>, ChangeSet {
    val size: Int
    val replaced: Int
    val totalChanges: Int
}
