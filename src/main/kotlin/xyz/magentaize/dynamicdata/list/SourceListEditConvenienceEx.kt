package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.list.internal.EditDiff

fun <T> EditableObservableList<T>.editDiff(
    items: Iterable<T>,
    equalityComparer: (T, T) -> Boolean = { o1, o2 -> o1 == o2 }
) =
    EditDiff(this, equalityComparer).edit(items)

fun <T> EditableObservableList<T>.add(item: T) =
    edit { it.add(item) }

fun <T> EditableObservableList<T>.add(index: Int, item: T) =
    edit { it.add(index, item) }

fun <T> EditableObservableList<T>.addRange(items: Iterable<T>) =
    edit { it.addAll(items) }

fun <T> EditableObservableList<T>.addRange(
    items: Iterable<T>,
    index: Int
) =
    edit { it.addAll(index, items) }

fun <T> EditableObservableList<T>.remove(item: T): Boolean {
    var removed = false
    edit { removed = it.remove(item) }
    return removed
}

fun <T> EditableObservableList<T>.removeAt(index: Int) =
    edit { it.removeAt(index) }

fun <T> EditableObservableList<T>.removeRange(index: Int, count: Int) =
    edit { it.removeAll(index, count) }

fun <T> EditableObservableList<T>.removeAll(items: Iterable<T>) =
    edit { it.removeMany(items) }

fun <T> EditableObservableList<T>.clear() =
    edit { it.clear() }

fun <T> EditableObservableList<T>.replace(original: T, destination: T) =
    edit { it.replace(original, destination) }

fun <T> EditableObservableList<T>.replaceAt(index: Int, item: T) =
    edit { it[index] = item }

fun <T> EditableObservableList<T>.move(original: Int, destination: Int) =
    edit { it.move(original, destination) }
