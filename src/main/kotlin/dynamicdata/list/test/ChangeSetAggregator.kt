package dynamicdata.list.test

import dynamicdata.list.IChangeSet
import dynamicdata.list.IObservableList
import dynamicdata.list.asObservableList
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

class ChangeSetAggregator<T>(source: Observable<IChangeSet<T>>) : Disposable {
    val data: IObservableList<T>
    val messages: List<IChangeSet<T>>
        get() = _messages.toList()

    private val _messages = mutableListOf<IChangeSet<T>>()
    private val cleanUp: Disposable
    private lateinit var error: Throwable
    private var disposed = false

    init {
        val published = source.publish()
        data = published.asObservableList()
        val result = published.subscribe({ _messages.add(it) }, { error = it })
        val connected = published.connect()

        cleanUp = CompositeDisposable(data, connected, result)
    }

    override fun dispose() {
        if (disposed) {
            return
        }

        synchronized(this) {
            if (disposed) {
                return
            }
            disposed = true
        }

        cleanUp.dispose()
    }

    override fun isDisposed(): Boolean {
        return disposed
    }
}
