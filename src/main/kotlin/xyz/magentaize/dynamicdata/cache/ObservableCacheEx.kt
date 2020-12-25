package xyz.magentaize.dynamicdata.cache

import xyz.magentaize.dynamicdata.kernel.ConnectionStatus
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import xyz.magentaize.dynamicdata.cache.internal.*
import xyz.magentaize.dynamicdata.cache.internal.Combiner
import xyz.magentaize.dynamicdata.cache.internal.StatusMonitor
import xyz.magentaize.dynamicdata.cache.internal.Transformer
import xyz.magentaize.dynamicdata.cache.internal.TransformerWithForcedTransform
import xyz.magentaize.dynamicdata.kernel.Optional
import xyz.magentaize.dynamicdata.kernel.subscribeBy
import xyz.magentaize.dynamicdata.list.ObservableList
import xyz.magentaize.dynamicdata.list.asObservableList
import xyz.magentaize.dynamicdata.list.transform

fun <T> Observable<T>.monitorStatus(): Observable<ConnectionStatus> =
    StatusMonitor(this).run()

fun <K, V> Observable<ChangeSet<K, V>>.add(
    vararg others: Observable<ChangeSet<K, V>>
): Observable<ChangeSet<K, V>> {
    require(others.isNotEmpty())

    return this.combine(CombineOperator.And, *others)
}

fun <K, V> Iterable<Observable<ChangeSet<K, V>>>.add(): Observable<ChangeSet<K, V>> =
    combine(CombineOperator.And)

fun <K, V> ObservableList<Observable<ChangeSet<K, V>>>.add(): Observable<ChangeSet<K, V>> =
    combine(CombineOperator.And)

@JvmName("AndCache")
fun <K, V> ObservableList<ObservableCache<K, V>>.add(): Observable<ChangeSet<K, V>> =
    combine(CombineOperator.And)

fun <K, V> Observable<ChangeSet<K, V>>.or(
    vararg others: Observable<ChangeSet<K, V>>
): Observable<ChangeSet<K, V>> {
    require(others.isNotEmpty())

    return this.combine(CombineOperator.Or, *others)
}

fun <K, V> Iterable<Observable<ChangeSet<K, V>>>.or(): Observable<ChangeSet<K, V>> =
    combine(CombineOperator.Or)

fun <K, V> ObservableList<Observable<ChangeSet<K, V>>>.or(): Observable<ChangeSet<K, V>> =
    combine(CombineOperator.Or)

@JvmName("OrCache")
fun <K, V> ObservableList<ObservableCache<K, V>>.or(): Observable<ChangeSet<K, V>> =
    combine(CombineOperator.Or)

fun <K, V> Observable<ChangeSet<K, V>>.xor(
    vararg others: Observable<ChangeSet<K, V>>
): Observable<ChangeSet<K, V>> {
    require(others.isNotEmpty())

    return this.combine(CombineOperator.Xor, *others)
}

fun <K, V> Iterable<Observable<ChangeSet<K, V>>>.xor(): Observable<ChangeSet<K, V>> =
    combine(CombineOperator.Xor)

fun <K, V> ObservableList<Observable<ChangeSet<K, V>>>.xor(): Observable<ChangeSet<K, V>> =
    combine(CombineOperator.Xor)

@JvmName("XorCache")
fun <K, V> ObservableList<ObservableCache<K, V>>.xor(): Observable<ChangeSet<K, V>> =
    combine(CombineOperator.Xor)

fun <K, V> Observable<ChangeSet<K, V>>.except(
    vararg others: Observable<ChangeSet<K, V>>
): Observable<ChangeSet<K, V>> {
    require(others.isNotEmpty())

    return this.combine(CombineOperator.Except, *others)
}

fun <K, V> Iterable<Observable<ChangeSet<K, V>>>.except(): Observable<ChangeSet<K, V>> =
    combine(CombineOperator.Except)

fun <K, V> ObservableList<Observable<ChangeSet<K, V>>>.except(): Observable<ChangeSet<K, V>> =
    combine(CombineOperator.Except)

@JvmName("ExceptCache")
fun <K, V> ObservableList<ObservableCache<K, V>>.except(): Observable<ChangeSet<K, V>> =
    combine(CombineOperator.Except)

fun <K, V, C : ChangeSet<K, V>> Observable<C>.notEmpty(): Observable<C> =
    filter { it.size != 0 }

fun <K, E, R> Observable<ChangeSet<K, E>>.transform(
    factory: (K, E) -> R,
    forceTransform: Observable<(K, E) -> Boolean>? = null
): Observable<ChangeSet<K, R>> =
    transform({ key, current, _ -> factory(key, current) }, forceTransform)

fun <K, E, R> Observable<ChangeSet<K, E>>.transform(
    factory: (K, E, Optional<E>) -> R,
    forceTransform: Observable<(K, E) -> Boolean>? = null
): Observable<ChangeSet<K, R>> =
    if (forceTransform != null)
        TransformerWithForcedTransform(this, factory, forceTransform).run()
    else
        Transformer(this, factory).run()

fun <K, V> Observable<ChangeSet<K, V>>.disposeMany(): Observable<ChangeSet<K, V>> =
    DisposeMany(this) { t ->
        val d = t as? Disposable
        d?.dispose()
    }.run()

fun <K, V> Observable<ChangeSet<K, V>>.subscribeMany(factory: (K, V) -> Disposable): Observable<ChangeSet<K, V>> =
    SubscribeMany(this, factory).run()

fun <K, V, R> Observable<ChangeSet<K, V>>.mergeMany(selector: (V) -> Observable<R>): Observable<R> =
    MergeMany(this, selector).run()

fun <K, V, R> Observable<ChangeSet<K, V>>.mergeMany(selector: (K, V) -> Observable<R>): Observable<R> =
    MergeMany(this, selector).run()

private fun <K, V> Iterable<Observable<ChangeSet<K, V>>>.combine(type: CombineOperator): Observable<ChangeSet<K, V>> =
    Observable.create { emitter ->
        fun update(updates: ChangeSet<K, V>) {
            try {
                emitter.onNext(updates)
            } catch (ex: Throwable) {
                emitter.onError(ex)
            }
        }

        var d = Disposable.empty()
        try {
            val combiner = Combiner(type, ::update)
            d = combiner.run(this.toList())
        } catch (ex: Throwable) {
            emitter.onError(ex)
            emitter.onComplete()
        }

        emitter.setDisposable(d)
    }

private fun <K, V> Observable<ChangeSet<K, V>>.combine(
    type: CombineOperator,
    vararg target: Observable<ChangeSet<K, V>>
): Observable<ChangeSet<K, V>> =
    Observable.create { emitter ->
        fun update(updates: ChangeSet<K, V>) {
            try {
                emitter.onNext(updates)
            } catch (ex: Throwable) {
                emitter.onError(ex)
            }
        }

        var d = Disposable.empty()
        try {
            val list = target.toMutableList()
            list.add(0, this)

            val combiner = Combiner(type, ::update)
            d = combiner.run(list)
        } catch (ex: Throwable) {
            emitter.onError(ex)
            emitter.onComplete()
        }

        emitter.setDisposable(d)
    }

private fun <K, V> ObservableList<ObservableCache<K, V>>.combine(type: CombineOperator): Observable<ChangeSet<K, V>> =
    Observable.create { emitter ->
        val connections = this.connect().transform { it.connect() }.asObservableList()
        val sub = connections.combine(type).subscribeBy(emitter)
        emitter.setDisposable(CompositeDisposable(connections, sub))
    }
