package dynamicdata.list

interface IExtendedList<E> : MutableList<E> {
    fun addAll(elements: Iterable<E>): Boolean
    fun move(original: Int, destination: Int)
    fun removeAll(index: Int, count: Int)
    fun addAll(index: Int, items: Iterable<E>)
}
