package xyz.magentaize.dynamicdata.cache.internal

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import xyz.magentaize.dynamicdata.cache.*
import xyz.magentaize.dynamicdata.kernel.ObservableEx
import xyz.magentaize.dynamicdata.kernel.buffer
import xyz.magentaize.dynamicdata.kernel.subscribeBy
import kotlin.time.Duration

internal class AutoRefresh<K, V, T>(
    private val _source: Observable<ChangeSet<K, V>>,
    private val _evaluator: (K, V) -> Observable<T>,
    private val _duration: Duration = Duration.ZERO,
    private val _scheduler: Scheduler = Schedulers.computation()
) {
    fun run(): Observable<ChangeSet<K, V>> =
        ObservableEx.create { emitter ->
            val shared = _source.publish()

            // monitor each item observable and create change
            val changes = shared
                .mergeMany { k, v ->
                    _evaluator(k, v)
                        .map { Change(ChangeReason.Refresh, k, v) }
                }

            // create a change set, either buffered or one item at the time
            val refreshChanges = if (_duration == Duration.ZERO)
                changes.map { AnonymousChangeSet(listOf(it)) }
            else
                changes.buffer(_duration, _scheduler)
                    .filter { it.isNotEmpty() }
                    .map { AnonymousChangeSet(it) }

            // publish refreshes and underlying changes
            val publisher = shared
                .mergeWith(refreshChanges)
                .subscribeBy(emitter)

            return@create CompositeDisposable(
                publisher,
                shared.connect()
            )
        }
}