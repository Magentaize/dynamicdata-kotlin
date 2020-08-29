package dynamicdata.list.internal

import dynamicdata.list.ListChangeReason

internal data class UnifiedChange<T>(
    val reason: ListChangeReason,
    val current: T,
    val previous: T? = null
)
