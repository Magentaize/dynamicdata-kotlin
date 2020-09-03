package dynamicdata.list

import dynamicdata.list.internal.AnonymousObservableList
import io.reactivex.rxjava3.core.Observable

fun <T> Observable<IChangeSet<T>>.asObservableList(): IObservableList<T> =
    AnonymousObservableList(this)

fun <T> Observable<IChangeSet<T>>.notEmpty(): Observable<IChangeSet<T>> =
    this.filter { it.size != 0 }
