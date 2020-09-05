package dynamicdata.list

import io.reactivex.rxjava3.core.Observable

fun <T, R> ISourceList<T>.cast(selector: (T) -> R): Observable<IChangeSet<R>> =
    connect().cast(selector)