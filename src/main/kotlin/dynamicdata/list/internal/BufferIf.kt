package dynamicdata.list.internal

import dynamicdata.kernel.subscribeBy
import dynamicdata.list.ChangeSet
import dynamicdata.list.IChangeSet
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.disposables.SerialDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.TimeUnit

internal class BufferIf<T>(
    private val _source: Observable<IChangeSet<T>>,
    private val _pauseIfTrueSelector: Observable<Boolean>,
    private val _initialPauseState: Boolean,
    private val _timespan: Long = 0L,
    private val _unit: TimeUnit = TimeUnit.NANOSECONDS,
    private val _scheduler: Scheduler = Schedulers.computation()
) {
    fun run(): Observable<IChangeSet<T>> =
        Observable.create { emitter ->
            var paused = _initialPauseState
            var buffer = ChangeSet<T>()
            val timeoutSubscriber = SerialDisposable();
            val timeoutSubject = PublishSubject.create<Boolean>()

            val bufferSelector =
                Observable.just(_initialPauseState)
                    .concatWith(_pauseIfTrueSelector.mergeWith(timeoutSubject))
                    .observeOn(_scheduler)
                    .serialize()
                    .publish()

            val pause =
                bufferSelector.filter { it }
                    .subscribe {
                        paused = true
                        //add pause timeout if required
                        if (_timespan != 0L) {
                            timeoutSubscriber.set(
                                Observable.timer(_timespan, _unit, _scheduler)
                                    .map { false }
                                    .subscribeBy(timeoutSubject)
                            )
                        }
                    }

            val resume =
                bufferSelector.filter { !it }
                    .subscribe {
                        paused = false
                        //publish changes and clear buffer
                        if (buffer.size == 0)
                            return@subscribe

                        emitter.onNext(buffer)
                        buffer = ChangeSet<T>()

                        //kill off timeout if required
                        timeoutSubscriber.set(Disposable.empty())
                    }

            val updateSubscriber =
                _source.serialize()
                    .subscribe { updates ->
                        if (paused)
                            buffer.addAll(updates)
                        else
                            emitter.onNext(updates)
                    }

            val connected = bufferSelector.connect()

            val dispose = Disposable.fromAction {
                connected.dispose()
                pause.dispose()
                resume.dispose()
                updateSubscriber.dispose()
                timeoutSubject.onComplete()
                timeoutSubscriber.dispose()
            }

            emitter.setDisposable(dispose)
        }
}
