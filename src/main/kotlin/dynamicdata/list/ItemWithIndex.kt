package dynamicdata.list

data class ItemWithIndex<T>(
    val item: T,
    val index: Int
) {
    override fun toString(): String {
        return "$item ($index)"
    }
}
