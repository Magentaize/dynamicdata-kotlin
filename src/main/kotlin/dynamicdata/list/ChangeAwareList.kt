package dynamicdata.list

class ChangeAwareList<T> : IExtendedList<T> {
    private val lockObject = Any()
    private val innerList: MutableList<T>
    private var changes = ChangeSet<T>()

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
        synchronized(lockObject) {
//            if (changes.count() == 0)
//                TODO: return empty

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
        synchronized(lockObject) {
            changes = ChangeSet()
        }
    }

    //region Collection overrides

    private val last: Change<T>?
        get() = synchronized(lockObject) {
            if (changes.size == 0) null else changes[changes.size - 1]
        }

    protected fun insertItem(index: Int, item: T) {
        require(index in 0..innerList.size + 1)

        synchronized(lockObject) {
            val last = (ChangeAwareList<T>::last)(this)

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
    //endregion

    //region IList<T> implementation

    fun add(item: T): Boolean =
        synchronized(lockObject) {
            insertItem(innerList.size, item)
            true
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

        if(args.range.isEmpty())
            return false

        synchronized(lockObject){
            innerList.addAll(args.range)
            changes.add(args)
            return true
        }
    }

    override fun iterator(): MutableIterator<T> =
        synchronized(lockObject) {
            innerList.toMutableList().iterator()
        }

    //endregion
}
