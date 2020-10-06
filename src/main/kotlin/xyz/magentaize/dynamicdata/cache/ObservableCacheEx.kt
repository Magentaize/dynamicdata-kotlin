package xyz.magentaize.dynamicdata.cache

import xyz.magentaize.dynamicdata.cache.internal.StatusMonitor
import xyz.magentaize.dynamicdata.kernel.ConnectionStatus
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import xyz.magentaize.dynamicdata.cache.internal.CombineOperator
import xyz.magentaize.dynamicdata.cache.internal.Combiner
import xyz.magentaize.dynamicdata.kernel.subscribeBy
import xyz.magentaize.dynamicdata.list.ObservableList
import xyz.magentaize.dynamicdata.list.asObservableList
import xyz.magentaize.dynamicdata.list.transform

fun <T> Observable<T>.monitorStatus(): Observable<ConnectionStatus> =
    StatusMonitor(this).run()

fun <T, K> Observable<ChangeSet<T, K>>.add(
    vararg others: Observable<ChangeSet<T, K>>
): Observable<ChangeSet<T, K>> {
    require(others.isNotEmpty())

    return this.combine(CombineOperator.And, *others)
}

fun <T, K> Iterable<Observable<ChangeSet<T, K>>>.add(): Observable<ChangeSet<T, K>> =
    combine(CombineOperator.And)

fun <T, K> ObservableList<Observable<ChangeSet<T, K>>>.add(): Observable<ChangeSet<T, K>> =
    combine(CombineOperator.And)

@JvmName("AndCache")
fun <T, K> ObservableList<ObservableCache<T, K>>.add(): Observable<ChangeSet<T, K>> =
    combine(CombineOperator.And)

fun <T, K> Observable<ChangeSet<T, K>>.or(
    vararg others: Observable<ChangeSet<T, K>>
): Observable<ChangeSet<T, K>> {
    require(others.isNotEmpty())

    return this.combine(CombineOperator.Or, *others)
}

fun <T, K> Iterable<Observable<ChangeSet<T, K>>>.or(): Observable<ChangeSet<T, K>> =
    combine(CombineOperator.Or)

fun <T, K> ObservableList<Observable<ChangeSet<T, K>>>.or(): Observable<ChangeSet<T, K>> =
    combine(CombineOperator.Or)

@JvmName("OrCache")
fun <T, K> ObservableList<ObservableCache<T, K>>.or(): Observable<ChangeSet<T, K>> =
    combine(CombineOperator.Or)

fun <T, K> Observable<ChangeSet<T, K>>.xor(
    vararg others: Observable<ChangeSet<T, K>>
): Observable<ChangeSet<T, K>> {
    require(others.isNotEmpty())

    return this.combine(CombineOperator.Xor, *others)
}

fun <T, K> Iterable<Observable<ChangeSet<T, K>>>.xor(): Observable<ChangeSet<T, K>> =
    combine(CombineOperator.Xor)

fun <T, K> ObservableList<Observable<ChangeSet<T, K>>>.xor(): Observable<ChangeSet<T, K>> =
    combine(CombineOperator.Xor)

@JvmName("XorCache")
fun <T, K> ObservableList<ObservableCache<T, K>>.xor(): Observable<ChangeSet<T, K>> =
    combine(CombineOperator.Xor)

fun <T, K> Observable<ChangeSet<T, K>>.except(
    vararg others: Observable<ChangeSet<T, K>>
): Observable<ChangeSet<T, K>> {
    require(others.isNotEmpty())

    return this.combine(CombineOperator.Except, *others)
}

fun <T, K> Iterable<Observable<ChangeSet<T, K>>>.except(): Observable<ChangeSet<T, K>> =
    combine(CombineOperator.Except)

fun <T, K> ObservableList<Observable<ChangeSet<T, K>>>.except(): Observable<ChangeSet<T, K>> =
    combine(CombineOperator.Except)

@JvmName("ExceptCache")
fun <T, K> ObservableList<ObservableCache<T, K>>.except(): Observable<ChangeSet<T, K>> =
    combine(CombineOperator.Except)

private fun <T, K> Iterable<Observable<ChangeSet<T, K>>>.combine(type: CombineOperator): Observable<ChangeSet<T, K>> =
    Observable.create { emitter ->
        fun update(updates: ChangeSet<T, K>) {
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

private fun <T, K> Observable<ChangeSet<T, K>>.combine(
    type: CombineOperator,
    vararg target: Observable<ChangeSet<T, K>>
): Observable<ChangeSet<T, K>> =
    Observable.create { emitter ->
        fun update(updates: ChangeSet<T, K>) {
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

private fun <T, K> ObservableList<ObservableCache<T, K>>.combine(type: CombineOperator): Observable<ChangeSet<T, K>> =
    Observable.create { emitter ->
        val connections = this.connect().transform { it.connect() }.asObservableList()
        val sub = connections.combine(type).subscribeBy(emitter)
        emitter.setDisposable(CompositeDisposable(connections, sub))
    }
