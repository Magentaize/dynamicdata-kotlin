package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.binding.whenPropertyChanged
import xyz.magentaize.dynamicdata.cache.PageRequest
import xyz.magentaize.dynamicdata.cache.internal.CombineOperator
import xyz.magentaize.dynamicdata.kernel.NotifyPropertyChanged
import xyz.magentaize.dynamicdata.kernel.Optional
import xyz.magentaize.dynamicdata.list.internal.*
import xyz.magentaize.dynamicdata.list.internal.AnonymousObservableList
import xyz.magentaize.dynamicdata.list.internal.Combiner
import xyz.magentaize.dynamicdata.list.internal.MergeMany
import xyz.magentaize.dynamicdata.list.internal.Transformer
import xyz.magentaize.dynamicdata.list.linq.Reverser
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import kotlin.reflect.KProperty1
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

fun <T> Observable<ChangeSet<T>>.asObservableList(): ObservableList<T> =
    AnonymousObservableList(this)

fun <T> Observable<ChangeSet<T>>.notEmpty(): Observable<ChangeSet<T>> =
    this.filter { it.size != 0 }

fun <T : NotifyPropertyChanged> Observable<ChangeSet<T>>.autoRefresh(
    bufferTimeSpan: Long? = null,
    bufferTimeUnit: TimeUnit? = null,
    propertyChangeThrottleTimeSpan: Long? = null,
    propertyChangeThrottleTimeUnit: TimeUnit? = null,
    scheduler: Scheduler = Schedulers.computation()
): Observable<ChangeSet<T>> =
    autoRefreshOnObservable({
        if (propertyChangeThrottleTimeSpan == null)
            it.whenPropertyChanged()
        else
            it
                .whenPropertyChanged()
                .throttleWithTimeout(propertyChangeThrottleTimeSpan, propertyChangeThrottleTimeUnit, scheduler)
    }, bufferTimeSpan, bufferTimeUnit, scheduler)

fun <T : NotifyPropertyChanged, R> Observable<ChangeSet<T>>.autoRefresh(
    accessor: KProperty1<T, R>,
    bufferTimeSpan: Long? = null,
    bufferTimeUnit: TimeUnit? = null,
    propertyChangeThrottleTimeSpan: Long? = null,
    propertyChangeThrottleTimeUnit: TimeUnit? = null,
    scheduler: Scheduler = Schedulers.computation()
): Observable<ChangeSet<T>> =
    autoRefreshOnObservable({
        if (propertyChangeThrottleTimeSpan == null)
            it.whenPropertyChanged(accessor, false)
        else
            it
                .whenPropertyChanged(accessor, false)
                .throttleWithTimeout(propertyChangeThrottleTimeSpan, propertyChangeThrottleTimeUnit, scheduler)
    }, bufferTimeSpan, bufferTimeUnit, scheduler)

fun <T, R> Observable<ChangeSet<T>>.autoRefreshOnObservable(
    evaluator: (T) -> Observable<R>,
    bufferTimeSpan: Long? = null,
    unit: TimeUnit? = null,
    scheduler: Scheduler? = null
): Observable<ChangeSet<T>> =
    AutoRefresh(this, evaluator, bufferTimeSpan, unit, scheduler).run()

fun <T, R> Observable<ChangeSet<T>>.transform(
    transformFactory: (T) -> R
): Observable<ChangeSet<R>> =
    this.transformWithOptional(
        { t, _, _ -> transformFactory(t) }
    )

fun <T, R> Observable<ChangeSet<T>>.transform(
    transformFactory: (T) -> R,
    transformOnRefresh: Boolean = false
): Observable<ChangeSet<R>> =
    this.transformWithOptional(
        { t, _, _ -> transformFactory(t) },
        transformOnRefresh
    )

fun <T, R> Observable<ChangeSet<T>>.transformWithIndex(
    transformFactory: (T, Int) -> R
): Observable<ChangeSet<R>> =
    this.transformWithOptional(
        { t, _, idx -> transformFactory(t, idx) }
    )

fun <T, R> Observable<ChangeSet<T>>.transformWithIndex(
    transformFactory: (T, Int) -> R,
    transformOnRefresh: Boolean = false
): Observable<ChangeSet<R>> =
    this.transformWithOptional(
        { t, _, idx -> transformFactory(t, idx) },
        transformOnRefresh
    )

fun <T, R> Observable<ChangeSet<T>>.transformWithOptional(
    transformFactory: (T, Optional<R>) -> R,
    transformOnRefresh: Boolean = false
): Observable<ChangeSet<R>> =
    this.transformWithOptional(
        { t, prev, _ -> transformFactory(t, prev) },
        transformOnRefresh
    )

fun <T, R> Observable<ChangeSet<T>>.transformWithOptional(
    transformFactory: (T, Optional<R>, Int) -> R,
    transformOnRefresh: Boolean = false
): Observable<ChangeSet<R>> =
    Transformer(this, transformFactory, transformOnRefresh).run()

fun <T> Observable<ChangeSet<T>>.disposeMany(): Observable<ChangeSet<T>> =
    onItemRemoved {
        val d = it as? Disposable
        d?.dispose()
    }

fun <T> Observable<ChangeSet<T>>.onItemRemoved(action: (T) -> Unit): Observable<ChangeSet<T>> =
    OnBeingRemoved(this, action).run()

fun <T> Observable<ChangeSet<T>>.xor(vararg others: Observable<ChangeSet<T>>) =
    combine(CombineOperator.Xor, *others)

fun <T> Collection<Observable<ChangeSet<T>>>.xor(): Observable<ChangeSet<T>> =
    combine(CombineOperator.Xor)

fun <T> Observable<ChangeSet<T>>.or(vararg others: Observable<ChangeSet<T>>) =
    combine(CombineOperator.Or, *others)

fun <T> Collection<Observable<ChangeSet<T>>>.or(): Observable<ChangeSet<T>> =
    combine(CombineOperator.Or)

fun <T> Observable<ChangeSet<T>>.and(vararg others: Observable<ChangeSet<T>>) =
    combine(CombineOperator.And, *others)

fun <T> Collection<Observable<ChangeSet<T>>>.and(): Observable<ChangeSet<T>> =
    combine(CombineOperator.And)

fun <T> Observable<ChangeSet<T>>.except(vararg others: Observable<ChangeSet<T>>) =
    combine(CombineOperator.Except, *others)

fun <T> Collection<Observable<ChangeSet<T>>>.except(): Observable<ChangeSet<T>> =
    combine(CombineOperator.Except)

private fun <T> Collection<Observable<ChangeSet<T>>>.combine(type: CombineOperator): Observable<ChangeSet<T>> =
    Combiner(this, type).run()

private fun <T> Observable<ChangeSet<T>>.combine(
    type: CombineOperator,
    vararg others: Observable<ChangeSet<T>>
): Observable<ChangeSet<T>> {
    if (others.isEmpty())
        throw IllegalArgumentException("Must be at least one item to combine with")

    val items = listOf(this).union(others.toList()).toList()
    return Combiner(items, type).run()
}

fun <T> ObservableList<Observable<ChangeSet<T>>>.and(): Observable<ChangeSet<T>> =
    combine(CombineOperator.And)

fun <T> ObservableList<Observable<ChangeSet<T>>>.except(): Observable<ChangeSet<T>> =
    combine(CombineOperator.Except)

fun <T> ObservableList<Observable<ChangeSet<T>>>.or(): Observable<ChangeSet<T>> =
    combine(CombineOperator.Or)

fun <T> ObservableList<Observable<ChangeSet<T>>>.xor(): Observable<ChangeSet<T>> =
    combine(CombineOperator.Xor)

private fun <T> ObservableList<Observable<ChangeSet<T>>>.combine(type: CombineOperator): Observable<ChangeSet<T>> =
    DynamicCombiner(this, type).run()

fun <T> Observable<ChangeSet<T>>.subscribeMany(subscriptionFactory: (T) -> Disposable): Observable<ChangeSet<T>> =
    SubscribeMany(this, subscriptionFactory).run()

fun <T, R> Observable<ChangeSet<T>>.mergeMany(selector: (T) -> Observable<R>): Observable<R> =
    MergeMany(this, selector).run()

fun <T> Observable<ChangeSet<T>>.clone(target: MutableList<T>): Observable<ChangeSet<T>> =
    doOnEach { target.clone(it.value) }

fun <T, R> Observable<ChangeSet<T>>.cast(selector: (T) -> R): Observable<ChangeSet<R>> =
    map { it.transform(selector) }

fun <T> Observable<ChangeSet<T>>.skipInitial(): Observable<ChangeSet<T>> =
    deferUntilLoaded().skip(1)

//fun <T> Observable<IChangeSet<T>>.deferUntilLoaded(): IObservableList<T> =
//    connect

fun <T> Observable<ChangeSet<T>>.deferUntilLoaded(): Observable<ChangeSet<T>> =
    DeferUntilLoaded(this).run()

fun <T> Observable<List<ChangeSet<T>>>.flattenBufferResult(): Observable<ChangeSet<T>> =
    this.filter { it.isNotEmpty() }
        .map { AnonymousChangeSet(it.flatten()) }

fun <T> Observable<ChangeSet<T>>.bufferInitial(
    timespan: Long,
    unit: TimeUnit,
    scheduler: Scheduler = Schedulers.computation()
): Observable<ChangeSet<T>> =
    deferUntilLoaded()
        .publish { shared ->
            val initial = shared.buffer(timespan, unit, scheduler)
                .flattenBufferResult()
                .take(1)

            initial.concatWith(shared)
        }

fun <T> Observable<ChangeSet<T>>.bufferIf(
    pauseIfTrueSelector: Observable<Boolean>,
    scheduler: Scheduler = Schedulers.computation()
): Observable<ChangeSet<T>> =
    bufferIf(pauseIfTrueSelector, false, scheduler)

fun <T> Observable<ChangeSet<T>>.bufferIf(
    pauseIfTrueSelector: Observable<Boolean>,
    initialPauseState: Boolean,
    scheduler: Scheduler = Schedulers.computation()
): Observable<ChangeSet<T>> =
    bufferIf(pauseIfTrueSelector, initialPauseState, 0L, TimeUnit.NANOSECONDS, scheduler)

fun <T> Observable<ChangeSet<T>>.bufferIf(
    pauseIfTrueSelector: Observable<Boolean>,
    timespan: Long,
    unit: TimeUnit,
    scheduler: Scheduler = Schedulers.computation()
): Observable<ChangeSet<T>> =
    bufferIf(pauseIfTrueSelector, false, timespan, unit, scheduler)

fun <T> Observable<ChangeSet<T>>.bufferIf(
    pauseIfTrueSelector: Observable<Boolean>,
    initialPauseState: Boolean,
    timespan: Long = 0L,
    unit: TimeUnit = TimeUnit.NANOSECONDS,
    scheduler: Scheduler = Schedulers.computation()
): Observable<ChangeSet<T>> =
    BufferIf(this, pauseIfTrueSelector, initialPauseState, timespan, unit, scheduler).run()

fun <T, R> Observable<ChangeSet<T>>.distinctValues(selector: (T) -> R): Observable<ChangeSet<R>> =
    Distinct(this, selector).run()

fun <T> Observable<ChangeSet<T>>.whereReasonsAre(vararg reasons: ListChangeReason): Observable<ChangeSet<T>> {
    require(reasons.isNotEmpty()) { "Must enter at least 1 reason" }

    val matches = hashSetOf(*reasons)
    return map<ChangeSet<T>> {
        val filtered = it.filter { change -> matches.contains(change.reason) }.yieldWithoutIndex()
        AnonymousChangeSet(filtered.toList())
    }.notEmpty()
}

fun <T> Observable<ChangeSet<T>>.removeIndex(): Observable<ChangeSet<T>> =
    map { AnonymousChangeSet(it.yieldWithoutIndex().toList()) }

fun <T> Observable<ChangeSet<T>>.filterItem(predicate: (T) -> Boolean): Observable<ChangeSet<T>> =
    Filter(this, predicate).run()

fun <T> Observable<ChangeSet<T>>.filterItem(
    predicate: (T) -> Boolean,
    policy: ListFilterPolicy = ListFilterPolicy.CalculateDiff
): Observable<ChangeSet<T>> =
    Filter(this, predicate, policy).run()

fun <T> Observable<ChangeSet<T>>.filterItem(
    predicate: Observable<(T) -> Boolean>
): Observable<ChangeSet<T>> =
    Filter(this, predicate, ListFilterPolicy.CalculateDiff).run()

fun <T> Observable<ChangeSet<T>>.filterItem(
    predicate: Observable<(T) -> Boolean>,
    policy: ListFilterPolicy = ListFilterPolicy.CalculateDiff
): Observable<ChangeSet<T>> =
    Filter(this, predicate, policy).run()

fun <T> Observable<ChangeSet<T>>.sort(
    comparator: Comparator<T>,
    sortOption: SortOption = SortOption.None,
    resort: Observable<Unit> = Observable.never(),
    comparatorChanged: Observable<Comparator<T>> = Observable.never(),
    resetThreshold: Int = 50
): Observable<ChangeSet<T>> =
    Sort(this, comparator, sortOption, resort, comparatorChanged, resetThreshold).run()

fun <T> Observable<ChangeSet<T>>.sort(
    comparatorChanged: Observable<Comparator<T>> = Observable.never(),
    sortOption: SortOption = SortOption.None,
    resort: Observable<Unit> = Observable.never(),
    resetThreshold: Int = 50
): Observable<ChangeSet<T>> =
    Sort(this, compareBy { it.hashCode() }, sortOption, resort, comparatorChanged, resetThreshold).run()

fun <T> Observable<ChangeSet<T>>.toCollection(): Observable<List<T>> =
    queryWhenChanged { it }

fun <T, R> Observable<ChangeSet<T>>.queryWhenChanged(selector: (List<T>) -> R): Observable<R> =
    queryWhenChanged().map(selector)

fun <T> Observable<ChangeSet<T>>.queryWhenChanged(): Observable<List<T>> =
    QueryWhenChanged(this).run()

fun <T, K> Observable<ChangeSet<T>>.groupOnMutable(
    selector: (T) -> K
): Observable<ChangeSet<MutableGroup<T, K>>> =
    groupOnMutable(selector, Observable.never())

fun <T, K> Observable<ChangeSet<T>>.groupOnMutable(
    selector: (T) -> K,
    regroup: Observable<Unit>
): Observable<ChangeSet<MutableGroup<T, K>>> =
    GroupOnMutable(this, selector, regroup).run()

fun <T, K> Observable<ChangeSet<T>>.groupOn(
    selector: (T) -> K
): Observable<ChangeSet<Group<T, K>>> =
    groupOn(selector, Observable.never())

fun <T, K> Observable<ChangeSet<T>>.groupOn(
    selector: (T) -> K,
    regroup: Observable<Unit>
): Observable<ChangeSet<Group<T, K>>> =
    GroupOn(this, selector, regroup).run()

@ExperimentalTime
fun <T> EditableObservableList<T>.expireAfter(
    timeSelector: (T) -> Duration?,
    scheduler: Scheduler = Schedulers.computation()
): Observable<Iterable<T>> =
    expireAfter(timeSelector, null, scheduler)

@ExperimentalTime
fun <T> EditableObservableList<T>.expireAfter(
    timeSelector: (T) -> Duration?,
    pollingInterval: Duration? = null,
    scheduler: Scheduler = Schedulers.computation()
): Observable<Iterable<T>> {
    val limiter = ExpireAfter(this, timeSelector, pollingInterval, scheduler)
    return limiter.run().doOnEach { removeAll(it.value) }
}

fun <T> Observable<ChangeSet<T>>.forEachChange(
    action: (Change<T>) -> Unit
): Observable<ChangeSet<T>> =
    doOnEach { it.value.forEach(action) }

fun <T> Observable<ChangeSet<T>>.forEachItemChange(
    action: (ItemChange<T>) -> Unit
): Observable<ChangeSet<T>> =
    doOnEach { it.value.flatten().forEach(action) }

@ExperimentalTime
fun <T> Observable<Iterable<T>>.toObservableChangeSet(
    scheduler: Scheduler = Schedulers.computation()
): Observable<ChangeSet<T>> =
    toObservableChangeSet(null, -1, scheduler)

@ExperimentalTime
fun <T> Observable<Iterable<T>>.toObservableChangeSet(
    expireAfter: ((T) -> Duration?)?,
    limitSizeTo: Int,
    scheduler: Scheduler = Schedulers.computation()
): Observable<ChangeSet<T>> =
    ToObservableChangeSet(this, expireAfter, limitSizeTo, scheduler).run()

fun <T, K> Observable<ChangeSet<T>>.groupWithImmutableState(
    selector: (T) -> K,
    regrouper: Observable<Unit> = Observable.never()
): Observable<ChangeSet<Group<T, K>>> =
    GroupOn(this, selector, regrouper).run()

fun <T> Observable<ChangeSet<T>>.page(
    requests: Observable<PageRequest>
): Observable<ChangeSet<T>> =
    Page(this, requests).run()

fun <T, R> Observable<ChangeSet<T>>.transformMany(
    selector: (T) -> Iterable<R>
): Observable<ChangeSet<R>> =
    TransformMany(this, selector, null).run()

fun <T> Observable<out ObservableList<T>>.switch(): Observable<ChangeSet<T>> =
    map { it.connect() }.switchObservable()

fun <T> Observable<Observable<ChangeSet<T>>>.switchObservable(): Observable<ChangeSet<T>> =
    Switch(this).run()

fun <T> Observable<ChangeSet<T>>.populateInto(
    destination: EditableObservableList<T>
): Disposable =
    subscribe { change -> destination.edit { it.clone(change) } }

fun <T> Observable<ChangeSet<T>>.refCount(): Observable<ChangeSet<T>> =
    RefCount(this).run()

fun <T> Observable<ChangeSet<T>>.reverse(): Observable<ChangeSet<T>> =
    Reverser<T>().let {
        this.map { x ->
            val list = it.reverse(x)
            AnonymousChangeSet(list.asSequence().toList())
        }
    }

fun <T> EditableObservableList<T>.limitSizeTo(
    limit: Int,
    scheduler: Scheduler = Schedulers.computation()
): Observable<Iterable<T>> =
    require(limit > 0){"limit must be greater than zero."}.let {
        LimitSizeTo(this, limit, scheduler)
            .run()
            .doOnEach { this.removeAll(it.value) }
    }
