package dynamicdata.kernel

fun <T> MutableList<T>.removeRange(itemsToRemove: Iterable<T>) {
    val toRemoveList = itemsToRemove.toList()

    // match all indicies and and remove in reverse as it is more efficient
    val toRemove = this.indexOfMany(toRemoveList)
        .sortedByDescending { it.index }
        .toList()

    //if there are duplicates, it could be that an item exists in the
    //source collection more than once - in that case the fast remove
    //would remove each instance
    val hasDuplicates = toRemove.duplicates { it.value }.any()

    if (hasDuplicates)
        //Slow remove but safe
        toRemoveList.forEach { this.remove(it) }
    else
        //Fast remove because we know the index of all and we remove in order
        toRemove.forEach { this.removeAt(it.index) }
}
