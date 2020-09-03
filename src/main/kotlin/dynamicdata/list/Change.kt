package dynamicdata.list

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
            : this(reason, current, null, index)

    constructor(current: T, currentIndex: Int, previousIndex: Int)
            : this(
        ListChangeReason.Moved, ItemChange(ListChangeReason.Moved, current, null, currentIndex, previousIndex),
        RangeChange.empty()
    )

    constructor(
        reason: ListChangeReason, current: T, previous: T?,
        currentIndex: Int = -1, previousIndex: Int = -1
    ) : this(reason, ItemChange(reason, current, previous, currentIndex, previousIndex), RangeChange.empty()) {
        if (reason == ListChangeReason.Add && previous != null)
            throw IllegalArgumentException("For ChangeReason.Add, a previous value cannot be specified")

        if (reason == ListChangeReason.Replace && previous == null)
            throw IllegalArgumentException("For ChangeReason.Change, must supply previous value")

        if (reason == ListChangeReason.Refresh && currentIndex < 0)
            throw  IllegalArgumentException("For ChangeReason.Refresh, must supply and index")
    }
}
