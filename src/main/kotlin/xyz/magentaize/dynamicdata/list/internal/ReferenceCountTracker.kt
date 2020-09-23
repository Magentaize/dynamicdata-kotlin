package xyz.magentaize.dynamicdata.list.internal

internal class ReferenceCountTracker<T> {
    private val referenceCounts = LinkedHashMap<T, Int>()
    val items: Iterable<T>
        get() = referenceCounts.keys

    fun get(key: T): Int? =
        referenceCounts[key]

    /*
        Increments the reference count for the item.  Returns true when reference count goes from 0 to 1.
    */
    fun add(item: T): Boolean {
        val currentCount = referenceCounts[item]

        if (currentCount == null) {
            referenceCounts[item] = 1
            return true
        }

        referenceCounts[item] = currentCount + 1
        return false
    }

    fun clear() {
        referenceCounts.clear()
    }

    fun remove(item: T): Boolean {
        val currentCount = referenceCounts[item]

        if (currentCount == 1) {
            referenceCounts.remove(item)
            return true
        }

        referenceCounts[item] = currentCount!! - 1
        return false
    }

    fun contains(item: T) =
        referenceCounts.contains(item)
}
