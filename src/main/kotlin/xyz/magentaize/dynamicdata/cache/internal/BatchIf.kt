package xyz.magentaize.dynamicdata.cache.internal

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.disposables.SerialDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import xyz.magentaize.dynamicdata.cache.AnonymousChangeSet
import xyz.magentaize.dynamicdata.cache.ChangeSet
import xyz.magentaize.dynamicdata.kernel.ObservableEx
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

internal class BatchIf<K, V>(
    private val _source: Observable<ChangeSet<K, V>>,
    private val _pauseIfTrueSelector: Observable<Boolean>,
    private val _timeout: Duration = Duration.ZERO,
    private val _initialPauseState: Boolean = false,
    private val _intervalTimer: Observable<Unit> = Observable.never(),
    private val _scheduler: Scheduler = Schedulers.computation()
) {
    fun run(): Observable<ChangeSet<K, V>> =
        ObservableEx.create { emitter ->
            val batchedChanges = mutableListOf<ChangeSet<K, V>>()
            var paused = _initialPauseState
            val timeoutDisposer = SerialDisposable()
            val intervalTimerDisposer = SerialDisposable()

            fun resumeAction() {
                if (batchedChanges.isEmpty()) return

                val resultBatch = AnonymousChangeSet<K, V>(batchedChanges.map { it.size }.sum())
                resultBatch.addAll(batchedChanges.flatten())
                emitter.onNext(resultBatch)
                batchedChanges.clear()
            }

            fun intervalFunction(): Disposable =
                _intervalTimer.doFinally { paused = false }
                    .subscribe {
                        paused = false
                        resumeAction()
                        if (_intervalTimer !== Observable.never<Unit>()) paused = true
                    }

            if (_intervalTimer != Observable.never<Unit>()) intervalTimerDisposer.set(intervalFunction())

            val pausedHandler = _pauseIfTrueSelector
                .subscribe {
                    paused = it
                    if (!it) {
                        // pause window has closed, so reset timer
                        if (_timeout != Duration.ZERO) timeoutDisposer.set(Disposable.empty())

                        resumeAction()
                    } else {
                        if (_timeout != Duration.ZERO)
                            timeoutDisposer.set(
                                Observable.timer(_timeout.toLongMilliseconds(), TimeUnit.MILLISECONDS).subscribe {
                                    paused = false
                                    resumeAction()
                                })
                    }
                }

            val publisher = _source.subscribe {
                batchedChanges.add(it)
                if (!paused) resumeAction()
            }

            return@create CompositeDisposable(publisher, pausedHandler, timeoutDisposer, intervalTimerDisposer)
        }
}