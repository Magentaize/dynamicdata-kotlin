package xyz.magentaize.dynamicdata.cache

import io.reactivex.rxjava3.core.Observable
import xyz.magentaize.dynamicdata.cache.test.ChangeSetAggregator

fun <K, V> Observable<ChangeSet<K, V>>.asAggregator(): ChangeSetAggregator<K, V> =
    ChangeSetAggregator(this)