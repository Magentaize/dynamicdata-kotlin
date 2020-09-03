package dynamicdata.list

open class ChangeAwareList<T> : IExtendedList<T> {
    private val lock = Any()
    private val innerList: MutableList<T>
    private var changes = ChangeSet<T>(emptyList())

    constructor(capacity: Int = -1) {
        innerList = if (capacity > 0) ArrayList(capacity) else ArrayList()
    }

    constructor(items: Iterable<T>) {
        innerList = items.toMutableList()

        if (innerList.any())
            changes.add(Change(ListChangeReason.AddRange, innerList.toList()))
    }

    constructor(list: ChangeAwareList<T>, copyChanges: Boolean) {
        innerList = list.innerList.toMutableList()

        if (copyChanges)
            changes = ChangeSet(list.changes)
    }

    override val size: Int
        get() = innerList.size

    fun captureChanges(): IChangeSet<T> {
        var retValue: ChangeSet<T>
        synchronized(lock) {
            if (changes.count() == 0)
                return ChangeSet.empty()

            retValue = changes
            val totalChanges = retValue.totalChanges
            if (innerList.count() == 0 && retValue.removes == totalChanges && totalChanges > 1) {
                val removed = retValue.unified().map { it.current }
                retValue = ChangeSet(listOf(Change(ListChangeReason.Clear, removed)))
            }

            clearChanges()
        }

        return retValue
    }

    /**
     * Clears the changes (for testing).
     */
    internal fun clearChanges() {
        synchronized(lock) {
            changes = ChangeSet(emptyList())
        }
    }

    //region Collection overrides

    private val last: Change<T>?
        get() = synchronized(lock) {
            if (changes.size == 0) null else changes[changes.size - 1]
        }

    override fun add(index: Int, item: T) {
        require(index in 0..innerList.size + 1)

        synchronized(lock) {
            val last = last

            if (last != null && last.reason == ListChangeReason.Add) {
                val firstOfBatch = changes.size - 1;
                val previousItem = last.item

                if (index == previousItem.currentIndex)
                    changes[firstOfBatch] = Change(ListChangeReason.AddRange, listOf(item, previousItem.current), index)
                else if (index == previousItem.currentIndex + 1)
                    changes[firstOfBatch] =
                        Change(ListChangeReason.AddRange, listOf(previousItem.current, item), previousItem.currentIndex)
                else
                    changes.add(Change(ListChangeReason.Add, item, index))

            } else if (last != null && last.reason == ListChangeReason.AddRange) {
                val range = last.range
                val min = maxOf(range.index - 1, 0)
                val max = range.index + range.size
                val isPartOfRange = index in min..max

                if (!isPartOfRange)
                    changes.add(Change(ListChangeReason.Add, item, index))
                else {
                    var insertPosition = index - range.index
                    if (insertPosition < 0)
                        insertPosition = 0
                    else if (insertPosition >= range.size)
                        insertPosition = range.size

                    range.add(insertPosition, item)

                    if (index < range.index)
                        range.setStartingIndex(index)
                }
            } else
                changes.add(Change(ListChangeReason.Add, item, index))

            innerList.add(index, item)
        }
    }

    fun addAll(index: Int, items: Iterable<T>): Boolean {
        val args = Change(ListChangeReason.AddRange, items, index)

        if (args.range.isEmpty()) return false

        synchronized(lock) {
            changes.add(args)
            innerList.addAll(index, args.range)
        }

        onInsertItems(index, args.range)

        return true
    }
    //endregion

    //region IList<T> implementation

    override fun add(item: T): Boolean =
        synchronized(lock) {
            this.add(innerList.size, item)
            true
        }

    /*
    DO NOT USE return element
    */
    override fun removeAt(index: Int): T {
        require(index >= 0)

        synchronized(lock) {
            if (index > innerList.size)
                throw IndexOutOfBoundsException("index cannot be greater than the size of the collection")

            removeItem(index)
        }

        return null as T
    }

    override fun remove(item: T): Boolean =
        synchronized(lock) {
            val index = innerList.indexOf(item)
            if (index < 0)
                false
            else {
                removeItem(index, item)
                true
            }
        }

    fun removeAll(index: Int, count: Int) {
        val args: Change<T>
        synchronized(lock) {
            val toIndex = index + count
            if (index >= innerList.size || toIndex > innerList.size)
                throw IndexOutOfBoundsException("index")

            val toRemove = innerList.drop(index).take(count).toList()
            if (toRemove.isEmpty()) return

            args = Change(ListChangeReason.RemoveRange, toRemove, index)
            changes.add(args)
            innerList.subList(index, toIndex).clear()
        }

        onRemoveItems(index, args.range)
    }

    protected fun removeItem(index: Int) =
        synchronized(lock) {
            val item = innerList[index]
            removeItem(index, item)
        }

    protected open fun removeItem(index: Int, item: T) {
        require(index >= 0)

        synchronized(lock) {
            if (index > innerList.size)
                throw IllegalArgumentException("index cannot be greater than the size of the collection")

            val last = last
            when (last?.reason) {
                ListChangeReason.Remove -> {
                    val firstOfBatch = changes.size - 1
                    val previousItem = last.item

                    when (index) {
                        previousItem.currentIndex -> changes[firstOfBatch] =
                            Change(ListChangeReason.RemoveRange, listOf(previousItem.current, item), index)
                        previousItem.currentIndex - 1
                            //Nb: double check this one as it is the same as clause above. Can it be correct?
                        -> changes[firstOfBatch] =
                            Change(ListChangeReason.RemoveRange, listOf(item, previousItem.current), index)
                        else -> changes.add(Change(ListChangeReason.Remove, item, index))
                    }
                }
                ListChangeReason.RemoveRange -> {
                    val range = last.range

                    //add to the end of the previous batch
                    when (range.index) {
                        index
                            //removed in order
                        -> range.add(item)
                        index - 1 -> changes.add(Change(ListChangeReason.Remove, item, index))
                        index + 1 -> {
                            //removed in reverse order
                            range.add(0, item)
                            range.setStartingIndex(index)
                        }
                        else -> changes.add(Change(ListChangeReason.Remove, item, index))
                    }
                }
                else -> {
                    changes.add(Change(ListChangeReason.Remove, item, index))
                }
            }

            innerList.removeAt(index)
        }
    }

    override fun contains(element: T): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    fun addAll(elements: Iterable<T>): Boolean {
        val args = Change(ListChangeReason.AddRange, elements)

        if (args.range.isEmpty())
            return false

        synchronized(lock) {
            innerList.addAll(args.range)
            changes.add(args)
            return true
        }
    }

    fun refreshAt(index: Int) {
        require(index >= 0)

        synchronized(lock) {
            if (index > innerList.size)
                throw IllegalArgumentException("index cannot be greater than the size of the collection")

            changes.add(Change(ListChangeReason.Refresh, innerList[index], index))
        }
    }

    fun refresh(item: T): Boolean {
        val index = indexOf(item)
        if (index < 0) return false

        synchronized(lock) {
            changes.add(Change(ListChangeReason.Refresh, item, index))
        }

        return true
    }

    override fun indexOf(item: T): Int =
        synchronized(lock) {
            innerList.indexOf(item)
        }

    override operator fun iterator(): MutableIterator<T> =
        synchronized(lock) {
            innerList.toMutableList().iterator()
        }

    //endregion

    override fun move(original: Int, destination: Int) {
        if (original < 0)
            throw IllegalArgumentException("original cannot be negative")
        if (destination < 0)
            throw IllegalArgumentException("destination cannot be negative")

        synchronized(lock) {
            if (original > innerList.size)
                throw IllegalArgumentException("original cannot be greater than the size of the collection")
            if (destination > innerList.size)
                throw IllegalArgumentException("destination cannot be greater than the size of the collection")

            val item = innerList[original]
            innerList.removeAt(original)
            innerList.add(destination, item)
            changes.add(Change(item, destination, original))
        }
    }

    protected open fun onSetItem(index: Int, newItem: T, oldItem: T) {}

    protected open fun onInsertItems(startIndex: Int, items: Collection<T>) {}

    protected open fun onRemoveItems(startIndex: Int, items: Collection<T>) {}

    override fun get(index: Int): T =
        synchronized(lock) {
            innerList[index]
        }

    override fun lastIndexOf(element: T): Int {
        TODO("Not yet implemented")
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean =
        TODO("Not yet implemented")

    override fun addAll(elements: Collection<T>): Boolean {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun listIterator(): MutableListIterator<T> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): MutableListIterator<T> {
        TODO("Not yet implemented")
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        TODO("Not yet implemented")
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        TODO("Not yet implemented")
    }

    override fun set(index: Int, element: T): T {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> =
        synchronized(lock) {
            ArrayList(innerList.subList(fromIndex, toIndex))
        }
}
