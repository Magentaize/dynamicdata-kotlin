package dynamicdata.kernel

import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.core.Emitter
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.observables.ConnectableObservable
import io.reactivex.rxjava3.observers.SafeObserver
import java.util.*

internal fun <T> Observable<T>.subscribeBy(observer: Observer<T>): Disposable =
    subscribe(observer::onNext, observer::onError, observer::onComplete)

internal fun <T> Observable<T>.subscribeBy(emitter: ObservableEmitter<T>): Disposable =
    subscribe(emitter::onNext, emitter::onError, emitter::onComplete)

internal fun <T> ConnectableObservable<T>.subscribeBy(emitter: ObservableEmitter<T>): Disposable =
    subscribe(emitter::onNext, emitter::onError, emitter::onComplete)

//internal fun <T> Observable<T>.subscribeSafe(observer: Observer<in T>): Disposable {
//    val ob = observer as? SafeObserver<T>
//    return if (ob != null)
//        subscribeBy(ob)
//    else
//        subscribeBy(SafeObserver(observer))
//}
//
//internal fun <T> Observable<T>.subscribeSafe(emitter: Emitter<T>): Disposable =
//    subscribeBy(SafeObserver(AnonymousObserver(emitter)))

internal fun <T> Observable<T>.doOnEach(
    onNext: Consumer<in T>,
    onError: Consumer<in Throwable>
): Observable<T> {
    return doOnEach(AnonymousObserver(onNext, onError))
}
