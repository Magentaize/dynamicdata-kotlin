package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.ChangeSet

interface ChangeSet<T> : Iterable<Change<T>>, ChangeSet {
    companion object {
        private val INSTANCE = AnonymousChangeSet<Any>()

        @Suppress("UNCHECKED_CAST")
        fun <T> empty(): xyz.magentaize.dynamicdata.list.ChangeSet<T> {
            return INSTANCE as xyz.magentaize.dynamicdata.list.ChangeSet<T>
        }
    }

    val size: Int
    val replaced: Int
    val totalChanges: Int
}
