package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.kernel.Optional

fun <T> Change<T>.movedWithinRange(start: Int, end: Int): Boolean {
    val curr = item.currentIndex
    val prev = item.previousIndex

    return curr in start..end || prev in start..end
}

fun <T> List<T>.indexOfOptional(item: T): Optional<ItemWithIndex<T>> {
    val index = indexOf(item)
    return if (index < 0) Optional.empty() else Optional.of(ItemWithIndex(item, index))
}
