package dynamicdata.list.test

import dynamicdata.list.ChangeSet
import io.reactivex.rxjava3.core.Observable

fun <T> Observable<ChangeSet<T>>.asAggregator() =
    ChangeSetAggregator(this)
