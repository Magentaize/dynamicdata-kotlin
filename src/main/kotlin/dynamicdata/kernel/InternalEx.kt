package dynamicdata.kernel

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

fun Scheduler.scheduleRecurringAction(run: Runnable, delay: Long, unit: TimeUnit): Disposable =
    schedulePeriodicallyDirect(run, delay, delay, unit)

@ExperimentalTime
fun Scheduler.scheduleRecurringAction(run: Runnable, delay: Duration): Disposable {
    val ms = delay.toLongMilliseconds()
    return scheduleRecurringAction(run, ms, TimeUnit.MILLISECONDS)
}

