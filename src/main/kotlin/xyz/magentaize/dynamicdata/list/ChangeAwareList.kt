package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.kernel.Optional

open class ChangeAwareList<T>(items: Iterable<T> = emptyList()) : ExtendedList<T> {
    private val lock = Any()
    private val _innerList: MutableList<T> = items.toMutableList()
    private var _changes: AnonymousChangeSet<T>

    init {
        _changes = AnonymousChangeSet(emptyList())

        if (_innerList.any())
            _changes.add(Change(ListChangeReason.AddRange, _innerList.toList()))
    }

    constructor(list: ChangeAwareList<T>, copyChanges: Boolean) : this(list._innerList) {
        if (copyChanges)
            _changes = AnonymousChangeSet(list._changes)
    }

    override val size: Int
        get() = _innerList.size

    fun captureChanges(): ChangeSet<T> {
        var retValue: AnonymousChangeSet<T>
        synchronized(lock) {
            if (_changes.count() == 0)
                return AnonymousChangeSet.empty()

            retValue = _changes
            val totalChanges = retValue.totalChanges
            if (_innerList.count() == 0 && retValue.removes == totalChanges && totalChanges > 1) {
                val removed = retValue.unified().map { it.current }
                retValue = AnonymousChangeSet(listOf(Change(ListChangeReason.Clear, removed)))
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
            _changes = AnonymousChangeSet(emptyList())
        }
    }

    private val last: Change<T>?
        get() = synchronized(lock) {
            if (_changes.size == 0) null else _changes[_changes.size - 1]
        }

    override fun add(index: Int, item: T) {
        require(index in 0.._innerList.size + 1)

        synchronized(lock) {
            val last = last

            if (last != null && last.reason == ListChangeReason.Add) {
                val firstOfBatch = _changes.size - 1
                val previousItem = last.item

                if (index == previousItem.currentIndex)
                    _changes[firstOfBatch] =
                        Change(ListChangeReason.AddRange, listOf(item, previousItem.current), index)
                else if (index == previousItem.currentIndex + 1)
                    _changes[firstOfBatch] =
                        Change(ListChangeReason.AddRange, listOf(previousItem.current, item), previousItem.currentIndex)
                else
                    _changes.add(Change(ListChangeReason.Add, item, index))

            } else if (last != null && last.reason == ListChangeReason.AddRange) {
                val range = last.range
                val min = maxOf(range.index - 1, 0)
                val max = range.index + range.size
                val isPartOfRange = index in min..max

                if (!isPartOfRange)
                    _changes.add(Change(ListChangeReason.Add, item, index))
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
                _changes.add(Change(ListChangeReason.Add, item, index))

            _innerList.add(index, item)
        }
    }

    override fun addAll(index: Int, items: Iterable<T>) {
        addAll(index, items.toList())
    }

    override fun add(item: T): Boolean =
        synchronized(lock) {
            this.add(_innerList.size, item)
            true
        }

    /*
    DO NOT USE return element
    */
    override fun removeAt(index: Int): T {
        require(index >= 0)

        synchronized(lock) {
            if (index > _innerList.size)
                throw IndexOutOfBoundsException("index cannot be greater than the size of the collection")

            removeItem(index)
        }

        return null as T
    }

    override fun remove(item: T): Boolean =
        synchronized(lock) {
            val index = _innerList.indexOf(item)
            if (index < 0)
                false
            else {
                removeItem(index, item)
                true
            }
        }

    override fun removeAll(index: Int, count: Int) {
        val args: Change<T>
        synchronized(lock) {
            val toIndex = index + count
            if (index >= _innerList.size || toIndex > _innerList.size)
                throw IndexOutOfBoundsException("index")

            val toRemove = _innerList.drop(index).take(count).toList()
            if (toRemove.isEmpty()) return

            args = Change(ListChangeReason.RemoveRange, toRemove, index)
            _changes.add(args)
            _innerList.subList(index, toIndex).clear()
        }

        onRemoveItems(index, args.range)
    }

    protected fun removeItem(index: Int) =
        synchronized(lock) {
            val item = _innerList[index]
            removeItem(index, item)
        }

    protected open fun removeItem(index: Int, item: T) {
        require(index >= 0)

        synchronized(lock) {
            if (index > _innerList.size)
                throw IllegalArgumentException("index cannot be greater than the size of the collection")

            val last = last
            when (last?.reason) {
                ListChangeReason.Remove -> {
                    val firstOfBatch = _changes.size - 1
                    val previousItem = last.item

                    when (index) {
                        previousItem.currentIndex -> _changes[firstOfBatch] =
                            Change(ListChangeReason.RemoveRange, listOf(previousItem.current, item), index)
                        previousItem.currentIndex - 1
                            //Nb: double check this one as it is the same as clause above. Can it be correct?
                        -> _changes[firstOfBatch] =
                            Change(ListChangeReason.RemoveRange, listOf(item, previousItem.current), index)
                        else -> _changes.add(Change(ListChangeReason.Remove, item, index))
                    }
                }
                ListChangeReason.RemoveRange -> {
                    val range = last.range

                    //add to the end of the previous batch
                    when (range.index) {
                        index
                            //removed in order
                        -> range.add(item)
                        index - 1 -> _changes.add(Change(ListChangeReason.Remove, item, index))
                        index + 1 -> {
                            //removed in reverse order
                            range.add(0, item)
                            range.setStartingIndex(index)
                        }
                        else -> _changes.add(Change(ListChangeReason.Remove, item, index))
                    }
                }
                else -> {
                    _changes.add(Change(ListChangeReason.Remove, item, index))
                }
            }

            _innerList.removeAt(index)
        }
    }

    override fun contains(element: T): Boolean =
        synchronized(lock) {
            _innerList.contains(element)
        }

    override fun containsAll(elements: Collection<T>): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean =
        _innerList.isEmpty()

    override fun addAll(elements: Iterable<T>): Boolean {
        val args = Change(ListChangeReason.AddRange, elements)

        if (args.range.isEmpty())
            return false

        synchronized(lock) {
            _innerList.addAll(args.range)
            _changes.add(args)
            return true
        }
    }

    fun refreshAt(index: Int) {
        require(index >= 0)

        synchronized(lock) {
            if (index > _innerList.size)
                throw IllegalArgumentException("index cannot be greater than the size of the collection")

            _changes.add(Change(ListChangeReason.Refresh, _innerList[index], index))
        }
    }

    fun refresh(item: T): Boolean {
        val index = indexOf(item)
        if (index < 0) return false

        synchronized(lock) {
            _changes.add(Change(ListChangeReason.Refresh, item, index))
        }

        return true
    }

    fun refresh(item: T, index: Int) {
        require(index >= 0) { "index cannot be negative." }

        synchronized(lock) {
            require(index <= _innerList.size) { "index cannot be greater than the size of the collection." }

            val prev = _innerList[index]
            _innerList[index] = item

            _changes.add(Change(ListChangeReason.Refresh, item, Optional.of(prev), index))
        }
    }

    override fun indexOf(item: T): Int =
        synchronized(lock) {
            _innerList.indexOf(item)
        }

    override operator fun iterator(): MutableIterator<T> =
        synchronized(lock) {
            _innerList.toMutableList().iterator()
        }

    override fun move(original: Int, destination: Int) {
        if (original < 0)
            throw IllegalArgumentException("original cannot be negative")
        if (destination < 0)
            throw IllegalArgumentException("destination cannot be negative")

        synchronized(lock) {
            if (original > _innerList.size)
                throw IllegalArgumentException("original cannot be greater than the size of the collection")
            if (destination > _innerList.size)
                throw IllegalArgumentException("destination cannot be greater than the size of the collection")

            val item = _innerList[original]
            _innerList.removeAt(original)
            _innerList.add(destination, item)
            _changes.add(Change(item, destination, original))
        }
    }

    protected open fun onSetItem(index: Int, newItem: T, oldItem: T) {}

    protected open fun onInsertItems(startIndex: Int, items: Collection<T>) {}

    protected open fun onRemoveItems(startIndex: Int, items: Collection<T>) {}

    override operator fun get(index: Int): T =
        synchronized(lock) {
            _innerList[index]
        }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        val args = Change(ListChangeReason.AddRange, elements, index)
        if (args.range.isEmpty())
            return false

        synchronized(lock) {
            _changes.add(args)
            _innerList.addAll(index, args.range)
        }

        onInsertItems(index, args.range)

        return true
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val args = Change(ListChangeReason.AddRange, elements)

        if (args.range.isEmpty())
            return false

        synchronized(lock) {
            _changes.add(args)
            _innerList.addAll(args.range)
        }

        return true
    }

    override fun clear() =
        synchronized(lock) {
            if (_innerList.size == 0)
                return@synchronized

            val toRemove = _innerList.toList()
            _changes.add(Change(ListChangeReason.Clear, toRemove))
            _innerList.clear()
        }


    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> =
        synchronized(lock) {
            ArrayList(_innerList.subList(fromIndex, toIndex))
        }

    override fun removeAll(elements: Collection<T>): Boolean {
        removeMany(elements)

        return true
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        TODO("Not yet implemented")
    }

    override fun set(index: Int, element: T): T =
        setItem(index, element)

    protected open fun setItem(index: Int, element: T): T {
        if (index < 0) throw IllegalArgumentException("index cannot be negative.")

        var previous: T
        synchronized(lock) {
            if (index > _innerList.size) throw IllegalArgumentException("index cannot be greater than the size of the collection.")

            previous = _innerList[index]
            _changes.add(Change(ListChangeReason.Replace, element, Optional.of(previous), index, index))
            _innerList[index] = element
        }

        onSetItem(index, element, previous)

        return previous
    }

    override fun lastIndexOf(element: T): Int {
        TODO("Not yet implemented")
    }

    override fun listIterator(): MutableListIterator<T> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): MutableListIterator<T> {
        TODO("Not yet implemented")
    }
}
