package dynamicdata.list

import dynamicdata.kernel.Optional

fun <T> List<T>.indexOfOptional(item: T): Optional<ItemWithIndex<T>> {
    val index = indexOf(item)
    return if (index < 0) Optional.empty() else Optional.of(ItemWithIndex(item, index))
}
