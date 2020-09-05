package dynamicdata.kernel

import io.reactivex.rxjava3.core.Emitter
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Action
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.internal.functions.Functions

internal class AnonymousObserver<T>(
    private val _onNext: Consumer<in T>,
    private val _onError: Consumer<in Throwable>,
    private val _onComplete: Action
) : Observer<T> {
    constructor(onNext: Consumer<in T>) :
            this(onNext, Functions.emptyConsumer(), Functions.EMPTY_ACTION)

    constructor(onNext: Consumer<in T>, onError: Consumer<in Throwable>) :
            this(onNext, onError, Functions.EMPTY_ACTION)

    constructor(emitter: Emitter<T>) :
            this(emitter::onNext, emitter::onError, emitter::onComplete)

    override fun onNext(t: T) =
        _onNext.accept(t)

    override fun onError(e: Throwable?) =
        _onError.accept(e)

    override fun onComplete() =
        _onComplete.run()

    override fun onSubscribe(d: Disposable?) {
    }
}