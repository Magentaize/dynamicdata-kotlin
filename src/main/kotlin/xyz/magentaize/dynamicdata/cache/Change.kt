package xyz.magentaize.dynamicdata.cache

import xyz.magentaize.dynamicdata.kernel.Optional
import java.lang.IllegalArgumentException
import java.util.*

data class Change<K, V>(
    val reason: ChangeReason,
    val key: K,
    val current: V,
    val previous: Optional<V>,
    val currentIndex: Int = -1,
    val previousIndex: Int = -1
) {
    init {
        if (reason == ChangeReason.Add && previous.hasValue)
            throw IllegalArgumentException("For ChangeReason.Add, a previous value cannot be specified")
        if (reason == ChangeReason.Update && !previous.hasValue)
            throw IllegalArgumentException("For ChangeReason.Change, must supply previous value")
    }

    constructor(reason: ChangeReason, key: K, current: V, index: Int = -1)
            : this(reason, key, current, Optional.empty(), index)

    constructor(key: K, current: V, currentIndex: Int, previousIndex: Int)
            : this(ChangeReason.Moved, key, current, Optional.empty(), currentIndex, previousIndex) {
        require(currentIndex > 0)
        require(previousIndex > 0)
    }

    override fun toString() =
        "$reason, Key: $key, Current: $current, Previous: $previous"
}
