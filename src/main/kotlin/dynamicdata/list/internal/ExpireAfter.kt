package dynamicdata.list.internal

import dynamicdata.kernel.scheduleRecurringAction
import dynamicdata.list.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import ru.yole.kxdate.fromNow
import ru.yole.kxdate.months
import ru.yole.kxdate.nanoseconds
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class ExpireAfter<T>(
    private val _source: ISourceList<T>,
    private val _expireAfter: (T) -> Duration?,
    private val _pollingInterval: Duration?,
    private val _scheduler: Scheduler
) {
    fun run(): Observable<Iterable<T>> =
        Observable.create { emitter ->
            var dateTime = Instant.ofEpochMilli(_scheduler.now(TimeUnit.MILLISECONDS))
            val orderItemWasAdded = AtomicInteger(-1)
            val autoRemover = _source.connect()
                .doOnEach { dateTime = Instant.ofEpochMilli(_scheduler.now(TimeUnit.MILLISECONDS)) }
                .cast {
                    val removeAt = _expireAfter(it)
                    val expireAt =
                        if (removeAt != null) dateTime.plusMillis(removeAt.toLongMilliseconds()) else Instant.MAX
                    ExpirableItem(it, expireAt, orderItemWasAdded.getAndIncrement())
                }
                .asObservableList()

            fun removal() {
                try {
                    val now = Instant.ofEpochMilli(_scheduler.now(TimeUnit.MILLISECONDS))
                    val toRemove = autoRemover.items
                        .filter { it.expireAt.isBefore(now) || it.expireAt == now }
                        .map { it.item }
                        .toList()

                    emitter.onNext(toRemove)
                } catch (e: Exception) {
                    emitter.onError(e)
                }
            }

            val removalSubscription =
                if (_pollingInterval != null)
                    _scheduler.scheduleRecurringAction(::removal, _pollingInterval)
                else
                    autoRemover.connect()
                        .distinctValues { it.expireAt }
                        .subscribeMany { time ->
                            val now = _scheduler.now(TimeUnit.MILLISECONDS)
                            val expireAt = if (time != Instant.MAX) time.minusMillis(now).toEpochMilli() else Long.MAX_VALUE
                            Observable.timer(expireAt, TimeUnit.MILLISECONDS, _scheduler)
                                .take(1)
                                .subscribe { removal() }
                        }
                        .subscribe()

            emitter.setDisposable(Disposable.fromAction {
                removalSubscription.dispose()
                autoRemover.dispose()
            })
        }
}
