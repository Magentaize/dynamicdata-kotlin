package xyz.magentaize.dynamicdata.kernel

import io.reactivex.rxjava3.schedulers.TestScheduler
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

fun TestScheduler.advanceTimeBy(duration: Duration) =
    this.advanceTimeBy(duration.toLongMilliseconds(), TimeUnit.MILLISECONDS)