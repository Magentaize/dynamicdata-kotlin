package dynamicdata.list

class RangeChange<T>(private val items: MutableList<T>, index: Int = -1) : Collection<T> by items {
    //private val items: MutableList<T> = items.toMutableList()

    constructor(items: Iterable<T>, index: Int = -1):this(items.toMutableList(), index)

    var index: Int = index
        private set

    fun add(item: T) =
        items.add(item)

    fun add(index: Int, item: T) =
        items.add(index, item)

    fun setStartingIndex(index: Int) {
        this.index = index
    }

    override fun iterator(): Iterator<T> =
        items.iterator()

    override fun contains(element: T): Boolean =
        items.contains(element)

    override fun containsAll(elements: Collection<T>): Boolean =
        items.containsAll(elements)

    override fun isEmpty(): Boolean =
        items.isEmpty()
}
