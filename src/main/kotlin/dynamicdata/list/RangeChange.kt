package dynamicdata.list

class RangeChange<T>(elements: Iterable<T>, index: Int = -1) : Collection<T> {
    companion object {
        private val INSTANCE: RangeChange<Any?> = RangeChange(emptyList())

        fun <T> empty(): RangeChange<T> {
            return INSTANCE as RangeChange<T>
        }
    }

    private val items: MutableList<T> = elements.toMutableList()

    //constructor(items: Iterable<T>, index: Int = -1):this(items.toMutableList(), index)

    var index: Int = index
        private set

    fun add(item: T) =
        items.add(item)

    fun add(index: Int, item: T) =
        items.add(index, item)

    fun setStartingIndex(index: Int) {
        this.index = index
    }

    override fun contains(element: T): Boolean =
        items.contains(element)

    override fun containsAll(elements: Collection<T>): Boolean =
        this.items.containsAll(elements)

    override fun isEmpty(): Boolean =
        items.isEmpty()

    override val size: Int
        get() = items.size

//    override fun iterator(): Iterator<T> =
//        items.iterator()

    override operator fun iterator(): Iterator<T> =
        items.iterator()


    override fun toString(): String {
        return "[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]"
    }
}
