package xyz.magentaize.dynamicdata.kernel

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit
import kotlin.time.Duration


class ObservableEx {
    companion object {
        @JvmStatic
        fun timer(duration: Duration, scheduler: Scheduler): Observable<Long> =
            Observable.timer(duration.toLongMilliseconds(), TimeUnit.MILLISECONDS, scheduler)

        @JvmStatic
        fun <T> create(source: (ObservableEmitter<T>) -> Disposable): Observable<T> =
            Observable.create { emitter ->
                val cleanUp = source(emitter)
                emitter.setDisposable(cleanUp)
            }
    }
}