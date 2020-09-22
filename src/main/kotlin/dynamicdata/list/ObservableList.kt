package dynamicdata.list

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable

interface ObservableList<T> : Disposable, Iterable<T> {
    fun connect(predicate: ((T) -> Boolean)? = null): Observable<ChangeSet<T>>
    fun preview(predicate: ((T) -> Boolean)? = null): Observable<ChangeSet<T>>
    val sizeChanged: Observable<Int>
    val items: Iterable<T>
    val size: Int
}
