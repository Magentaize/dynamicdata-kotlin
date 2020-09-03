package dynamicdata.list.test

import dynamicdata.list.IChangeSet
import io.reactivex.rxjava3.core.Observable

fun <T> Observable<IChangeSet<T>>.asAggregator() =
    ChangeSetAggregator<T>(this)
