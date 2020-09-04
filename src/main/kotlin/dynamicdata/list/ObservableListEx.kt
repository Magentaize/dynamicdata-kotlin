package dynamicdata.list

import dynamicdata.cache.internal.CombineOperator
import dynamicdata.list.internal.AnonymousObservableList
import dynamicdata.list.internal.Combiner
import io.reactivex.rxjava3.core.Observable

fun <T> Observable<IChangeSet<T>>.asObservableList(): IObservableList<T> =
    AnonymousObservableList(this)

fun <T> Observable<IChangeSet<T>>.notEmpty(): Observable<IChangeSet<T>> =
    this.filter { it.size != 0 }

fun <T> Observable<IChangeSet<T>>.and(vararg others: Observable<IChangeSet<T>>) =
    combine(CombineOperator.And, *others)

fun <T> Collection<Observable<IChangeSet<T>>>.and() =
    combine(CombineOperator.And)

private fun <T> Collection<Observable<IChangeSet<T>>>.combine(type: CombineOperator):Observable<IChangeSet<T>> =
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
