package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.kernel.subscribeBy
import xyz.magentaize.dynamicdata.list.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class ToObservableChangeSet<T>(
    private val _source: Observable<Iterable<T>>,
    private val _expireAfter: ((T) -> Duration?)?,
    private val _limitSizeTo: Int,
    private val _scheduler: Scheduler = Schedulers.computation()
) {
    fun run(): Observable<ChangeSet<T>> =
        Observable.create { emitter ->
            if (_expireAfter == null && _limitSizeTo < 1) {
                val d = _source.scan(ChangeAwareList<T>()) { state, latest ->
                    val items = latest.toList()
                    state.addAll(items)
                    return@scan state
                }
                    .skip(1)
                    .map { it.captureChanges() }
                    .subscribeBy(emitter)

                emitter.setDisposable(d)

                return@create
            }

            val orderItemWasAdded = AtomicLong(-1)
            val sourceList = ChangeAwareList<ExpirableItem<T>>()
            val sizeLimited =
                _source.scan(sourceList) { state, latest ->
                    val items = latest.toList()
                    val expirable = items.map { CreateExpirableItem(it, orderItemWasAdded) }
                    sourceList.addAll(expirable)

                    if (_limitSizeTo > 0 && state.size > _limitSizeTo) {
                        //remove oldest items [these will always be the first x in the list]
                        val toRemove = state.size - _limitSizeTo
                        state.removeAll(0, toRemove)
                    }

                    return@scan state
                }
                    .skip(1)
                    .map { it.captureChanges() }
                    .publish()

            val timeLimited = (if (_expireAfter == null) Observable.never() else sizeLimited)
                .filterItem { it.expireAt != Instant.MAX }
                .groupWithImmutableState({ it.expireAt })
                .mergeMany { g ->
                    val expireAt = g.key.minusMillis(_scheduler.now(TimeUnit.MILLISECONDS))
                    return@mergeMany Observable.timer(expireAt.toEpochMilli(), TimeUnit.MILLISECONDS, _scheduler)
                        .map { g }
                }
                .map {
                    sourceList.removeMany(it.items)
                    return@map sourceList.captureChanges()
                }

            val publisher = sizeLimited
                .mergeWith(timeLimited)
                .cast { it.item }
                .notEmpty()
                .subscribeBy(emitter)

            val d = CompositeDisposable(
                publisher,
                sizeLimited.connect()
            )

            emitter.setDisposable(d)
        }

    private fun CreateExpirableItem(latest: T, order: AtomicLong): ExpirableItem<T> {
        val now = Instant.ofEpochMilli(_scheduler.now(TimeUnit.MILLISECONDS))
        val remoteAt = _expireAfter?.invoke(latest)
        val expireAt = if (remoteAt != null) now.plusMillis(remoteAt.toLongMilliseconds()) else Instant.MAX
        return ExpirableItem(latest, expireAt, order.incrementAndGet())
    }
}
