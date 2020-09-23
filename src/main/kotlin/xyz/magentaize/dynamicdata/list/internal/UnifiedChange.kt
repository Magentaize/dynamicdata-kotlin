package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.kernel.Optional
import xyz.magentaize.dynamicdata.list.ListChangeReason

internal data class UnifiedChange<T>(
    val reason: ListChangeReason,
    val current: T,
    val previous: Optional<T> = Optional.empty()
)
