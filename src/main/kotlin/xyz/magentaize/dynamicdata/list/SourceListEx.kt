package xyz.magentaize.dynamicdata.list

import io.reactivex.rxjava3.core.Observable

fun <T, R> EditableObservableList<T>.cast(selector: (T) -> R): Observable<ChangeSet<R>> =
    connect().cast(selector)
