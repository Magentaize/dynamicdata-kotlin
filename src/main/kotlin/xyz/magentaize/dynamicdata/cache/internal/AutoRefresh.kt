package xyz.magentaize.dynamicdata.cache.internal

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import xyz.magentaize.dynamicdata.cache.ChangeSet
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

internal class AutoRefresh<K,V,T> @ExperimentalTime constructor(
    val _source: Observable<ChangeSet<K, V>>,
    val evaluator: (K,V)-> Observable<T>,
    val buffer: Duration?,
    val scheduler: Scheduler = Schedulers.computation()
) {
    fun run():Observable<ChangeSet<K, V>> =
        Observable.create { emitter ->
            val shared = _source.publish()

            // monitor each item observable and create change
            //val changes = shared.mergeWith {  }
            TODO()
        }
}