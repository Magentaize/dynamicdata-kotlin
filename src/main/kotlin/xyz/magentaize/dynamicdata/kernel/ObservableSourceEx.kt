package xyz.magentaize.dynamicdata.kernel

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import java.util.concurrent.TimeUnit
import kotlin.time.Duration


class ObservableEx {
    companion object {
        fun timer(duration: Duration, scheduler: Scheduler): Observable<Long> =
            Observable.timer(duration.toLongMilliseconds(), TimeUnit.MILLISECONDS, scheduler)
    }
}