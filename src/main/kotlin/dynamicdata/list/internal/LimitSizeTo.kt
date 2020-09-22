package dynamicdata.list.internal

import dynamicdata.list.ISourceList
import dynamicdata.list.toCollection
import dynamicdata.list.transform
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

internal class LimitSizeTo<T>(
    private val _source: ISourceList<T>,
    private val _limit: Int,
    private val _scheduler: Scheduler
) {
    fun run(): Observable<Iterable<T>> {
        val emptyList = emptyList<T>()
        val orderItemWasAdded = AtomicLong(-1)

        return _source.connect()
            .observeOn(_scheduler)
            .transform {
                ExpirableItem(
                    it,
                    Instant.ofEpochMilli(_scheduler.now(TimeUnit.MILLISECONDS)),
                    orderItemWasAdded.incrementAndGet()
                )
            }
            .toCollection()
            .map {
                val numberToExpire = it.size - _limit
                return@map if (numberToExpire < 0)
                    emptyList
                else
                    it.sortedWith(compareBy({ exp -> exp.expireAt }, { exp -> exp.index }))
                        .take(numberToExpire)
                        .map { item -> item.item }
                        //.toList()
            }
            .filter { it.isNotEmpty() }
            as Observable<Iterable<T>>
    }
}
