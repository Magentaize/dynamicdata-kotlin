package dynamicdata.list

data class ItemChange<T>
    (
    val reason: ListChangeReason, val current: T, val previous: T?,
    val currentIndex: Int = -1, val previousIndex: Int = -1
) {
    companion object {
        private val INSTANCE: ItemChange<Any?> = ItemChange(
            ListChangeReason.Add, null, null, -1, -1
        )

        fun <T> empty(): ItemChange<T> {
            return INSTANCE as ItemChange<T>
        }
    }

    override fun toString(): String =
        "Current: $current, Previous: $previous"
}
