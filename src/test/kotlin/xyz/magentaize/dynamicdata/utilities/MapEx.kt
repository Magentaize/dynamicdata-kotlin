package xyz.magentaize.dynamicdata.utilities

fun <T> Iterable<T>.recursiveMap(
    childSelector: (T) -> Iterable<T>
): Iterable<T> =
    recursiveMap(childSelector) { it -> it }

fun <T, R> Iterable<T>.recursiveMap(
    childSelector: (T) -> Iterable<T>,
    selector: (T) -> R
): Iterable<R> =
    recursiveMap(childSelector) { element, _, _ -> selector(element) }

fun <T, R> Iterable<T>.recursiveMap(
    childSelector: (T) -> Iterable<T>,
    selector: (T, Int, Int) -> R
): Iterable<R> =
    recursiveMap(childSelector, selector, 0)

fun <T, R> Iterable<T>.recursiveMap(
    childSelector: (T) -> Iterable<T>,
    selector: (T, Int, Int) -> R,
    depth: Int
): Iterable<R> =
    flatMapIndexed { index: Int, t: T ->
        Iterable {
            iterator {
                yield(selector(t, index, depth))
                yieldAll(childSelector(t).recursiveMap(childSelector, selector, depth + 1))
            }
        }
    }
