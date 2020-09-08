package dynamicdata.list

interface Grouping<T, K> {
    val key: K
    val items: Iterable<T>
    val size: Int
}
