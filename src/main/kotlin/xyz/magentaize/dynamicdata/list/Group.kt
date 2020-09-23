package xyz.magentaize.dynamicdata.list

interface Group<T, K> {
    val key: K
    val items: Iterable<T>
    val size: Int
}
