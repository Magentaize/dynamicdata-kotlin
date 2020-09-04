package dynamicdata.list

interface IExtendedList<E> : MutableCollection<E> {
    fun add(index: Int, item: E)
    fun addAll(elements: Iterable<E>): Boolean
    fun move(original: Int, destination: Int)
    fun removeAll(index: Int, count: Int)
    fun addAll(index: Int, items: Iterable<E>)
    fun removeAt(index: Int): E
    operator fun set(index: Int, element: E): E
}
