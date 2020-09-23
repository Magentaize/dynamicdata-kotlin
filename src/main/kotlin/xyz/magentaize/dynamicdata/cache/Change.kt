package xyz.magentaize.dynamicdata.cache

import java.lang.IllegalArgumentException
import java.util.*

data class Change<TObject, TKey>(
    val reason: ChangeReason,
    val key: TKey,
    val current: TObject,
    val previous: TObject?,
    val currentIndex: Int = -1,
    val previousIndex: Int = -1
) {
    init {
        if (reason == ChangeReason.Add && previous != null)
            throw IllegalArgumentException("For ChangeReason.Add, a previous value cannot be specified")
        if (reason == ChangeReason.Update && previous == null)
            throw IllegalArgumentException("For ChangeReason.Change, must supply previous value")
    }

    constructor(reason: ChangeReason, key: TKey, current: TObject, index: Int = -1)
            : this(reason, key, current, null, index)

    constructor(key: TKey, current: TObject, currentIndex: Int, previousIndex: Int)
            : this(ChangeReason.Moved, key, current, null, currentIndex, previousIndex) {
        require(currentIndex > 0)
        require(previousIndex > 0)
    }

    override fun toString() =
        "$reason, Key: $key, Current: $current, Previous: $previous"
}
