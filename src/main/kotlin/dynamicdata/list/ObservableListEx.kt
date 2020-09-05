package dynamicdata.list

import dynamicdata.cache.internal.CombineOperator
import dynamicdata.kernel.Optional
import dynamicdata.list.internal.*
import dynamicdata.list.internal.AnonymousObservableList
import dynamicdata.list.internal.Combiner
import dynamicdata.list.internal.MergeMany
import dynamicdata.list.internal.Transformer
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.beans.PropertyChangeListener
import java.util.concurrent.TimeUnit

fun <T> Observable<IChangeSet<T>>.asObservableList(): IObservableList<T> =
    AnonymousObservableList(this)

fun <T> Observable<IChangeSet<T>>.notEmpty(): Observable<IChangeSet<T>> =
    this.filter { it.size != 0 }

fun <T, R> PropertyChangeListener.whenPropertyChanged(
    propertyAccessor: (T) -> R,
    notifyOnInitialValue: Boolean = true,
    fallbackValue: (() -> R)? = null
) {
    TODO()
}

fun <T : PropertyChangeListener, R> Observable<IChangeSet<T>>.autoRefresh(
    propertyAccessor: (T) -> R,
    bufferTimeSpan: Long? = null,
    unit: TimeUnit? = null,
    propertyChangeThrottle: Long? = null,
    scheduler: Scheduler? = null
): Observable<IChangeSet<T>> =
    TODO()
//    autoRefreshOnObservable({t->
//        if(propertyChangeThrottle == null)
//            t.whenPropertyChanged()
//        else
//            t.whenPropertyChanged()
//    }, bufferTimeSpan, unit, scheduler)

//fun <T, R> Observable<IChangeSet<T>>.autoRefreshOnObservable(
//    evaluator: (T) -> Observable<R>,
//    bufferTimeSpan: Long? = null,
//    unit: TimeUnit? = null,
//    scheduler: Scheduler? = null
//): Observable<IChangeSet<T>> =
//    AutoRefresh(this, evaluator, bufferTimeSpan, unit, scheduler).run()

fun <T, R> Observable<IChangeSet<T>>.transform(
    transformFactory: (T) -> R
): Observable<IChangeSet<R>> =
    this.transform(
        { t, _, _ -> transformFactory(t) }
    )

fun <T, R> Observable<IChangeSet<T>>.transform(
    transformFactory: (T) -> R,
    transformOnRefresh: Boolean = false
): Observable<IChangeSet<R>> =
    this.transform(
        { t, _, _ -> transformFactory(t) },
        transformOnRefresh
    )

@JvmName("transformWithIndex")
fun <T, R> Observable<IChangeSet<T>>.transform(
    transformFactory: (T, Int) -> R,
    transformOnRefresh: Boolean = false
): Observable<IChangeSet<R>> =
    this.transform(
        { t, _, idx -> transformFactory(t, idx) },
        transformOnRefresh
    )

@JvmName("transformWithOptional")
fun <T, R> Observable<IChangeSet<T>>.transform(
    transformFactory: (T, Optional<R>) -> R,
    transformOnRefresh: Boolean = false
): Observable<IChangeSet<R>> =
    this.transform(
        { t, prev, _ -> transformFactory(t, prev) },
        transformOnRefresh
    )

fun <T, R> Observable<IChangeSet<T>>.transform(
    transformFactory: (T, Optional<R>, Int) -> R,
    transformOnRefresh: Boolean = false
): Observable<IChangeSet<R>> =
    Transformer(this, transformFactory, transformOnRefresh).run()

fun <T> Observable<IChangeSet<T>>.disposeMany(): Observable<IChangeSet<T>> =
    onItemRemoved {
        val d = it as? Disposable
        d?.dispose()
    }

fun <T> Observable<IChangeSet<T>>.onItemRemoved(action: (T) -> Unit): Observable<IChangeSet<T>> =
    OnBeingRemoved(this, action).run()

fun <T> Observable<IChangeSet<T>>.and(vararg others: Observable<IChangeSet<T>>) =
    combine(CombineOperator.And, *others)

fun <T> Collection<Observable<IChangeSet<T>>>.and(): Observable<IChangeSet<T>> =
    combine(CombineOperator.And)

private fun <T> Collection<Observable<IChangeSet<T>>>.combine(type: CombineOperator): Observable<IChangeSet<T>> =
    Combiner(this, type).run()

private fun <T> Observable<IChangeSet<T>>.combine(
    type: CombineOperator,
    vararg others: Observable<IChangeSet<T>>
): Observable<IChangeSet<T>> {
    if (others.isEmpty())
        throw IllegalArgumentException("Must be at least one item to combine with")

    val items = listOf(this).union(others.toList()).toList()
    return Combiner(items, type).run()
}

fun <T> IObservableList<Observable<IChangeSet<T>>>.and(): Observable<IChangeSet<T>> =
    combine(CombineOperator.And)

private fun <T> IObservableList<Observable<IChangeSet<T>>>.combine(type: CombineOperator): Observable<IChangeSet<T>> =
    DynamicCombiner(this, type).run()

fun <T> Observable<IChangeSet<T>>.subscribeMany(subscriptionFactory: (T) -> Disposable): Observable<IChangeSet<T>> =
    SubscribeMany(this, subscriptionFactory).run()

fun <T, R> Observable<IChangeSet<T>>.mergeMany(selector: (T) -> Observable<R>): Observable<R> =
    MergeMany(this, selector).run()

fun <T> Observable<IChangeSet<T>>.clone(target: IExtendedList<T>): Observable<IChangeSet<T>> {
    TODO()
    //return this.doOnEach{ target.clone(it)}
}

fun <T, R> Observable<IChangeSet<T>>.cast(selector: (T) -> R): Observable<IChangeSet<R>> =
    map { it.transform(selector) }

fun <T> Observable<IChangeSet<T>>.skipInitial(): Observable<IChangeSet<T>> =
    deferUntilLoaded().skip(1)

//fun <T> Observable<IChangeSet<T>>.deferUntilLoaded(): IObservableList<T> =
//    connect

fun <T> Observable<IChangeSet<T>>.deferUntilLoaded(): Observable<IChangeSet<T>> =
    DeferUntilLoaded(this).run()

fun <T> Observable<List<IChangeSet<T>>>.flattenBufferResult(): Observable<IChangeSet<T>> =
    this.filter { it.isNotEmpty() }
        .map { ChangeSet(it.flatten()) }

fun <T> Observable<IChangeSet<T>>.bufferInitial(
    timespan: Long,
    unit: TimeUnit,
    scheduler: Scheduler = Schedulers.computation()
): Observable<IChangeSet<T>> =
    deferUntilLoaded()
        .publish { shared ->
            val initial = shared.buffer(timespan, unit, scheduler)
                .flattenBufferResult()
                .take(1)

            initial.concatWith(shared)
        }

fun <T> Observable<IChangeSet<T>>.bufferIf(
    pauseIfTrueSelector: Observable<Boolean>,
    scheduler: Scheduler = Schedulers.computation()
): Observable<IChangeSet<T>> =
    bufferIf(pauseIfTrueSelector, false, scheduler)

fun <T> Observable<IChangeSet<T>>.bufferIf(
    pauseIfTrueSelector: Observable<Boolean>,
    initialPauseState: Boolean,
    scheduler: Scheduler = Schedulers.computation()
): Observable<IChangeSet<T>> =
    bufferIf(pauseIfTrueSelector, initialPauseState, 0L, TimeUnit.NANOSECONDS, scheduler)

fun <T> Observable<IChangeSet<T>>.bufferIf(
    pauseIfTrueSelector: Observable<Boolean>,
    timespan: Long,
    unit: TimeUnit,
    scheduler: Scheduler = Schedulers.computation()
): Observable<IChangeSet<T>> =
    bufferIf(pauseIfTrueSelector, false, timespan, unit, scheduler)

fun <T> Observable<IChangeSet<T>>.bufferIf(
    pauseIfTrueSelector: Observable<Boolean>,
    initialPauseState: Boolean,
    timespan: Long = 0L,
    unit: TimeUnit = TimeUnit.NANOSECONDS,
    scheduler: Scheduler = Schedulers.computation()
): Observable<IChangeSet<T>> =
    BufferIf(this, pauseIfTrueSelector, initialPauseState, timespan, unit, scheduler).run()

fun <T, R> Observable<IChangeSet<T>>.distinctValues(selector: (T) -> R): Observable<IChangeSet<R>> =
    Distinct(this, selector).run()

fun <T> Observable<IChangeSet<T>>.whereReasonsAre(vararg reasons: ListChangeReason): Observable<IChangeSet<T>> {
    require(reasons.isNotEmpty()) { "Must enter at least 1 reason" }

    val matches = hashSetOf(*reasons)
    return map<IChangeSet<T>> {
        val filtered = it.filter { change -> matches.contains(change.reason) }.yieldWithoutIndex()
        ChangeSet(filtered.toList())
    }.notEmpty()
}

fun <T> Observable<IChangeSet<T>>.forEachItemChange(action: (ItemChange<T>) -> Unit): Observable<IChangeSet<T>> =
    doOnEach { it.value.flatten().forEach(action) }
