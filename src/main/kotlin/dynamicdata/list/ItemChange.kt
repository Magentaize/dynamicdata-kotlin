package dynamicdata.list

import dynamicdata.kernel.Optional

data class ItemChange<T>
    (
    val reason: ListChangeReason, val current: T, val previous: Optional<T>,
    val currentIndex: Int = -1, val previousIndex: Int = -1
) {
    companion object {
        private val INSTANCE: ItemChange<*> = ItemChange(
            ListChangeReason.Add, null, Optional.empty(), -1, -1
        )

        fun <T> empty(): ItemChange<T> {
            return INSTANCE as ItemChange<T>
        }
    }

    constructor(reason: ListChangeReason, current: T, currentIndex: Int)
            : this(reason, current, Optional.empty(), currentIndex, -1)

    override fun toString(): String =
        "Current: $current, Previous: $previous"
}
