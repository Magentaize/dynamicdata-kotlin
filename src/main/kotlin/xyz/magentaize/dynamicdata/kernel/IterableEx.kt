package xyz.magentaize.dynamicdata.kernel

import net.servicestack.func.Func.join

fun <T, R> Iterable<T>.duplicates(selector: (T) -> R) =
    this.groupBy(selector)
        .filterValues { it.size > 1 }
        .flatMap { it.value }

fun <T> Iterable<T>.indexOfMany(itemsToFind: Iterable<T>) =
    this.indexOfMany(itemsToFind) { idx: Int, v: T ->
        IndexedValue(idx, v)
    }

fun <TObject, TResult> Iterable<TObject>.indexOfMany(
    itemsToFind: Iterable<TObject>,
    selector: (Int, TObject) -> TResult
) =
    join(this.withIndex(), itemsToFind.toList()) { idx, e ->
        idx.value == e
    }
        .map { selector(it.A.index, it.A.value) }

fun <T, R> Iterable<T>.aggregate(
    seed: R,
    func: (R, T) -> R
): R {
    var accumulate = seed
    forEach { accumulate = func(accumulate, it) }
    return accumulate
}
