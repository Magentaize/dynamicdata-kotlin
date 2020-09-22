package dynamicdata.aggregation

import dynamicdata.kernel.aggregate
import dynamicdata.list.ChangeSet
import io.reactivex.rxjava3.core.Observable

fun <T> Observable<ChangeSet<T>>.forAggregation(): Observable<AggregateChangeSet<T>> =
    map { AggregateEnumerator(it) }

internal fun <T, R> Observable<AggregateChangeSet<T>>.accumulate(
    seed: R,
    accessor: (T) -> R,
    addAction: (R, R) -> R,
    removeAction: (R, R) -> R
): Observable<R> =
    scan(seed) { state, changes ->
        changes.aggregate(state) { current, accumulate ->
            if (accumulate.type == AggregateType.Add)
                addAction(current, accessor(accumulate.item))
            else
                removeAction(current, accessor(accumulate.item))
        }
    }.skip(1)
