package dynamicdata.list.internal

import dynamicdata.kernel.Optional
import dynamicdata.list.ListChangeReason

internal data class UnifiedChange<T>(
    val reason: ListChangeReason,
    val current: T,
    val previous: Optional<T> = Optional.empty()
)
