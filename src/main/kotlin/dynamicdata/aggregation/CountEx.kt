package dynamicdata.aggregation

import dynamicdata.list.ChangeSet
import io.reactivex.rxjava3.core.Observable


fun <T> Observable<ChangeSet<T>>.countItem(): Observable<Int> =
    forAggregation().countAggregateItem()

fun <T> Observable<AggregateChangeSet<T>>.countAggregateItem(): Observable<Int> =
    accumulate(0,
        { 1 },
        { current, increment -> current + increment },
        { current, increment -> current - increment }
    )
