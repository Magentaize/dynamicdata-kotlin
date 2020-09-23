package xyz.magentaize.dynamicdata.list.test

import xyz.magentaize.dynamicdata.list.ChangeSet
import io.reactivex.rxjava3.core.Observable

fun <T> Observable<ChangeSet<T>>.asAggregator() =
    ChangeSetAggregator(this)
