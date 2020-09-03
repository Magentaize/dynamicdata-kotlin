package dynamicdata.list

interface IExtendedList<E> : Collection<E>, MutableList<E> {
    fun move(original: Int, destination: Int)
}
