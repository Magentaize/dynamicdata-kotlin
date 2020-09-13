package dynamicdata.list

import dynamicdata.list.internal.EditDiff

fun <T> ISourceList<T>.editDiff(
    items: Iterable<T>,
    equalityComparer: (T, T) -> Boolean = { o1, o2 -> o1 == o2 }
) =
    EditDiff(this, equalityComparer).edit(items)

fun <T> ISourceList<T>.add(item: T) =
    edit { it.add(item) }

fun <T> ISourceList<T>.addRange(items: Iterable<T>) =
    edit { it.addAll(items) }

fun <T> ISourceList<T>.remove(item: T): Boolean {
    var removed = false
    edit { removed = it.remove(item) }
    return removed
}

fun <T> ISourceList<T>.removeAt(index: Int) =
    edit { it.removeAt(index) }

fun <T> ISourceList<T>.removeRange(index: Int, count: Int) =
    edit { it.removeAll(index, count) }

fun <T> ISourceList<T>.removeAll(items: Iterable<T>) =
    edit { it.removeMany(items) }

fun <T> ISourceList<T>.clear() =
    edit { it.clear() }

fun <T> ISourceList<T>.replace(original: T, destination: T) =
    edit { it.replace(original, destination) }

fun <T> ISourceList<T>.replaceAt(index: Int, item: T) =
    edit { it[index] = item }
