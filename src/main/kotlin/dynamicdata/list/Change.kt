package dynamicdata.list

import dynamicdata.kernel.Optional
import kotlin.IllegalArgumentException

data class Change<T>(
    val reason: ListChangeReason,
    val item: ItemChange<T>,
    val range: RangeChange<T>
) {
    val type: ChangeType
        get() = reason.getChangeType()

    constructor(reason: ListChangeReason, items: Iterable<T>, index: Int = -1)
            : this(reason, ItemChange.empty(), RangeChange(items, index))

    constructor(reason: ListChangeReason, current: T, index: Int = -1)
            : this(reason, current, Optional.empty(), index)

    constructor(current: T, currentIndex: Int, previousIndex: Int)
            : this(
        ListChangeReason.Moved,
        ItemChange(ListChangeReason.Moved, current, Optional.empty(), currentIndex, previousIndex),
        RangeChange.empty()
    )

    constructor(
        reason: ListChangeReason, current: T, previous: Optional<T>,
        currentIndex: Int = -1, previousIndex: Int = -1
    ) : this(reason, ItemChange(reason, current, previous, currentIndex, previousIndex), RangeChange.empty()) {
        if (reason == ListChangeReason.Add && previous.hasValue)
            throw IllegalArgumentException("For ChangeReason.Add, a previous value cannot be specified")

        if (reason == ListChangeReason.Replace && !previous.hasValue)
            throw IllegalArgumentException("For ChangeReason.Change, must supply previous value")

        if (reason == ListChangeReason.Refresh && currentIndex < 0)
            throw  IllegalArgumentException("For ChangeReason.Refresh, must supply and index")
    }

    override fun toString(): String {
        return if (range.isEmpty()) "$reason. current: ${item.current}, previous: ${item.previous}"
        else "$reason. ${range.size} changes"
    }
}
