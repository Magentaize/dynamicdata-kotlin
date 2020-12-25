package xyz.magentaize.dynamicdata.cache.internal

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import xyz.magentaize.dynamicdata.cache.*
import xyz.magentaize.dynamicdata.kernel.Error
import xyz.magentaize.dynamicdata.kernel.Optional
import xyz.magentaize.dynamicdata.kernel.Stub
import xyz.magentaize.dynamicdata.kernel.subscribeBy

internal class TransformerWithForcedTransform<K, E, R>(
    private val _source: Observable<ChangeSet<K, E>>,
    private val _factory: (K, E, Optional<E>) -> R,
    private val _forceTransform: Observable<(K, E) -> Boolean>,
    private val _exceptionCallback: (Error<K, E>) -> Unit = (Stub)::EMPTY_CALLBACK,
) {
    fun run(): Observable<ChangeSet<K, R>> =
        Observable.create { emitter ->
            val shared = _source.publish()

            // capture all items so we can apply a forced transform
            val cache = Cache<K, E>()
            val cacheLoader = shared.subscribe { cache.clone(it) }

            // create change set of items where force refresh is applied
            val refresher = _forceTransform
                .map { captureChanges(cache, it) }
                .map { AnonymousChangeSet(it.asSequence().toList()) }
                .notEmpty()

            val sourceAndRefreshes = shared.mergeWith(refresher)

            val transform = Transformer(sourceAndRefreshes, _factory, _exceptionCallback, true).run()

            emitter.setDisposable(
                CompositeDisposable(
                    cacheLoader,
                    transform.subscribeBy(emitter),
                    shared.connect()
                )
            )
        }

    private fun captureChanges(cache: Cache<K, E>, shouldTransform: ((K, E) -> Boolean)): Iterator<Change<K, E>> =
        iterator {
            cache.keyValues.forEach { (k, v) ->
                if (shouldTransform(k, v))
                    yield(Change(ChangeReason.Refresh, k, v))
            }
        }
}