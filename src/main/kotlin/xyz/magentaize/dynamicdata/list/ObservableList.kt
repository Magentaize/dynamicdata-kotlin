package xyz.magentaize.dynamicdata.list

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import xyz.magentaize.dynamicdata.kernel.Stub
import xyz.magentaize.dynamicdata.list.internal.AnonymousObservableList

interface ObservableList<T> : Disposable, Iterable<T> {
    companion object {
        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T> empty(): ObservableList<T> = INSTANCE as ObservableList<T>

        private val INSTANCE = AnonymousObservableList(Observable.never<ChangeSet<Any>>())
    }

    fun connect(predicate: (T) -> Boolean = Stub.emptyFilter()): Observable<ChangeSet<T>>
    fun preview(predicate: (T) -> Boolean = Stub.emptyFilter()): Observable<ChangeSet<T>>
    val sizeChanged: Observable<Int>
    val items: Iterable<T>
    val size: Int
}
