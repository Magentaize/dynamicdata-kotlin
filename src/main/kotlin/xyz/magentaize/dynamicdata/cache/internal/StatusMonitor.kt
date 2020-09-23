package xyz.magentaize.dynamicdata.cache.internal

import xyz.magentaize.dynamicdata.kernel.ConnectionStatus
import xyz.magentaize.dynamicdata.kernel.subscribeBy
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject

internal class StatusMonitor<T>(
    private val _source: Observable<T>
) {
    fun run(): Observable<ConnectionStatus> =
        Observable.create { emitter ->
            val statusSubject = PublishSubject.create<ConnectionStatus>()
            var status = ConnectionStatus.Pending

            fun error(e: Throwable) {
                status = ConnectionStatus.Errored
                statusSubject.onNext(status)
                emitter.onError(e)
            }

            fun completion() {
                if (status == ConnectionStatus.Errored) return

                status = ConnectionStatus.Completed
                statusSubject.onNext(status)
            }

            fun updated() {
                if (status != ConnectionStatus.Pending) return

                status = ConnectionStatus.Loaded
                statusSubject.onNext(status)
            }

            val monitor = _source.subscribe({ updated() }, ::error, ::completion)

            val subscription = statusSubject
                .startWithItem(status)
                .distinctUntilChanged()
                .subscribeBy(emitter)

            val dispose = Disposable.fromAction {
                statusSubject.onComplete()
                monitor.dispose()
                subscription.dispose()
            }

            emitter.setDisposable(dispose)
        }
}
