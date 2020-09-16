package dynamicdata.list

import dynamicdata.aggregation.AggregateChangeSet
import dynamicdata.aggregation.AggregateEnumerator
import dynamicdata.binding.whenPropertyChanged
import dynamicdata.cache.internal.CombineOperator
import dynamicdata.kernel.INotifyPropertyChanged
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
import java.util.concurrent.TimeUnit
import kotlin.reflect.KProperty1
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

fun <T> Observable<IChangeSet<T>>.asObservableList(): IObservableList<T> =
    AnonymousObservableList(this)

fun <T> Observable<IChangeSet<T>>.notEmpty(): Observable<IChangeSet<T>> =
    this.filter { it.size != 0 }

fun <T : INotifyPropertyChanged> Observable<IChangeSet<T>>.autoRefresh(
    bufferTimeSpan: Long? = null,
    bufferTimeUnit: TimeUnit? = null,
    propertyChangeThrottleTimeSpan: Long? = null,
    propertyChangeThrottleTimeUnit: TimeUnit? = null,
    scheduler: Scheduler = Schedulers.computation()
): Observable<IChangeSet<T>> =
    autoRefreshOnObservable({
        if (propertyChangeThrottleTimeSpan == null)
            it.whenPropertyChanged()
        else
            it
                .whenPropertyChanged()
                .throttleWithTimeout(propertyChangeThrottleTimeSpan, propertyChangeThrottleTimeUnit, scheduler)
    }, bufferTimeSpan, bufferTimeUnit, scheduler)

fun <T : INotifyPropertyChanged, R> Observable<IChangeSet<T>>.autoRefresh(
    accessor: KProperty1<T, R>,
    bufferTimeSpan: Long? = null,
    bufferTimeUnit: TimeUnit? = null,
    propertyChangeThrottleTimeSpan: Long? = null,
    propertyChangeThrottleTimeUnit: TimeUnit? = null,
    scheduler: Scheduler = Schedulers.computation()
): Observable<IChangeSet<T>> =
    autoRefreshOnObservable({
        if (propertyChangeThrottleTimeSpan == null)
            it.whenPropertyChanged(accessor, false)
        else
            it
                .whenPropertyChanged(accessor, false)
                .throttleWithTimeout(propertyChangeThrottleTimeSpan, propertyChangeThrottleTimeUnit, scheduler)
    }, bufferTimeSpan, bufferTimeUnit, scheduler)

fun <T, R> Observable<IChangeSet<T>>.autoRefreshOnObservable(
    evaluator: (T) -> Observable<R>,
    bufferTimeSpan: Long? = null,
    unit: TimeUnit? = null,
    scheduler: Scheduler? = null
): Observable<IChangeSet<T>> =
    AutoRefresh(this, evaluator, bufferTimeSpan, unit, scheduler).run()

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
    transformFactory: (T, Int) -> R
): Observable<IChangeSet<R>> =
    this.transform(
        { t, _, idx -> transformFactory(t, idx) }
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

fun <T> Observable<IChangeSet<T>>.or(vararg others: Observable<IChangeSet<T>>) =
    combine(CombineOperator.Or, *others)

fun <T> Collection<Observable<IChangeSet<T>>>.or(): Observable<IChangeSet<T>> =
    combine(CombineOperator.Or)

fun <T> Observable<IChangeSet<T>>.and(vararg others: Observable<IChangeSet<T>>) =
    combine(CombineOperator.And, *others)

fun <T> Collection<Observable<IChangeSet<T>>>.and(): Observable<IChangeSet<T>> =
    combine(CombineOperator.And)

fun <T> Observable<IChangeSet<T>>.except(vararg others: Observable<IChangeSet<T>>) =
    combine(CombineOperator.Except, *others)

fun <T> Collection<Observable<IChangeSet<T>>>.except(): Observable<IChangeSet<T>> =
    combine(CombineOperator.Except)

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

fun <T> IObservableList<Observable<IChangeSet<T>>>.except(): Observable<IChangeSet<T>> =
    combine(CombineOperator.Except)

fun <T> IObservableList<Observable<IChangeSet<T>>>.or(): Observable<IChangeSet<T>> =
    combine(CombineOperator.Or)

fun <T> IObservableList<Observable<IChangeSet<T>>>.xor(): Observable<IChangeSet<T>> =
    combine(CombineOperator.Xor)

private fun <T> IObservableList<Observable<IChangeSet<T>>>.combine(type: CombineOperator): Observable<IChangeSet<T>> =
    DynamicCombiner(this, type).run()

fun <T> Observable<IChangeSet<T>>.subscribeMany(subscriptionFactory: (T) -> Disposable): Observable<IChangeSet<T>> =
    SubscribeMany(this, subscriptionFactory).run()

fun <T, R> Observable<IChangeSet<T>>.mergeMany(selector: (T) -> Observable<R>): Observable<R> =
    MergeMany(this, selector).run()

fun <T> Observable<IChangeSet<T>>.clone(target: MutableList<T>): Observable<IChangeSet<T>> =
    doOnEach { target.clone(it.value) }

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

fun <T> Observable<IChangeSet<T>>.removeIndex(): Observable<IChangeSet<T>> =
    map { ChangeSet(it.yieldWithoutIndex().toList()) }

fun <T> Observable<IChangeSet<T>>.filterItem(predicate: (T) -> Boolean): Observable<IChangeSet<T>> =
    Filter(this, predicate).run()

fun <T> Observable<IChangeSet<T>>.filterItem(
    predicate: (T) -> Boolean,
    policy: ListFilterPolicy = ListFilterPolicy.CalculateDiff
): Observable<IChangeSet<T>> =
    Filter(this, predicate, policy).run()

fun <T> Observable<IChangeSet<T>>.filterItem(
    predicate: Observable<(T) -> Boolean>
): Observable<IChangeSet<T>> =
    Filter(this, predicate, ListFilterPolicy.CalculateDiff).run()

fun <T> Observable<IChangeSet<T>>.filterItem(
    predicate: Observable<(T) -> Boolean>,
    policy: ListFilterPolicy = ListFilterPolicy.CalculateDiff
): Observable<IChangeSet<T>> =
    Filter(this, predicate, policy).run()

fun <T> Observable<IChangeSet<T>>.sort(
    comparator: Comparator<T>,
    sortOption: SortOption = SortOption.None,
    resort: Observable<Unit> = Observable.never(),
    comparatorChanged: Observable<Comparator<T>> = Observable.never(),
    resetThreshold: Int = 50
): Observable<IChangeSet<T>> =
    Sort(this, comparator, sortOption, resort, comparatorChanged, resetThreshold).run()

fun <T> Observable<IChangeSet<T>>.toCollection(): Observable<List<T>> =
    queryWhenChanged { it }

fun <T, R> Observable<IChangeSet<T>>.queryWhenChanged(selector: (List<T>) -> R): Observable<R> =
    queryWhenChanged().map(selector)

fun <T> Observable<IChangeSet<T>>.queryWhenChanged(): Observable<List<T>> =
    QueryWhenChanged(this).run()

fun <T, K> Observable<IChangeSet<T>>.groupOnMutable(
    selector: (T) -> K
): Observable<IChangeSet<MutableGroup<T, K>>> =
    groupOnMutable(selector, Observable.never())

fun <T, K> Observable<IChangeSet<T>>.groupOnMutable(
    selector: (T) -> K,
    regroup: Observable<Unit>
): Observable<IChangeSet<MutableGroup<T, K>>> =
    GroupOnMutable(this, selector, regroup).run()

fun <T, K> Observable<IChangeSet<T>>.groupOn(
    selector: (T) -> K
): Observable<IChangeSet<Group<T, K>>> =
    groupOn(selector, Observable.never())

fun <T, K> Observable<IChangeSet<T>>.groupOn(
    selector: (T) -> K,
    regroup: Observable<Unit>
): Observable<IChangeSet<Group<T, K>>> =
    GroupOn(this, selector, regroup).run()

@ExperimentalTime
fun <T> ISourceList<T>.expireAfter(
    timeSelector: (T) -> Duration?,
    scheduler: Scheduler = Schedulers.computation()
): Observable<Iterable<T>> =
    expireAfter(timeSelector, null, scheduler)

@ExperimentalTime
fun <T> ISourceList<T>.expireAfter(
    timeSelector: (T) -> Duration?,
    pollingInterval: Duration? = null,
    scheduler: Scheduler = Schedulers.computation()
): Observable<Iterable<T>> {
    val limiter = ExpireAfter(this, timeSelector, pollingInterval, scheduler)
    return limiter.run().doOnEach { removeAll(it.value) }
}

fun <T> Observable<IChangeSet<T>>.forEachChange(
    action: (Change<T>) -> Unit
): Observable<IChangeSet<T>> =
    doOnEach { it.value.forEach(action) }

fun <T> Observable<IChangeSet<T>>.forEachItemChange(
    action: (ItemChange<T>) -> Unit
): Observable<IChangeSet<T>> =
    doOnEach { it.value.flatten().forEach(action) }

@ExperimentalTime
fun <T> Observable<Iterable<T>>.toObservableChangeSet(
    scheduler: Scheduler = Schedulers.computation()
): Observable<IChangeSet<T>> =
    toObservableChangeSet(null, -1, scheduler)

@ExperimentalTime
fun <T> Observable<Iterable<T>>.toObservableChangeSet(
    expireAfter: ((T) -> Duration?)?,
    limitSizeTo: Int,
    scheduler: Scheduler = Schedulers.computation()
): Observable<IChangeSet<T>> =
    ToObservableChangeSet(this, expireAfter, limitSizeTo, scheduler).run()

fun <T, K> Observable<IChangeSet<T>>.groupWithImmutableState(
    selector: (T) -> K,
    regrouper: Observable<Unit> = Observable.never()
): Observable<IChangeSet<Group<T, K>>> =
    GroupOn(this, selector, regrouper).run()
