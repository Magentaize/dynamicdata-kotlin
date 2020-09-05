package dynamicdata.list

import dynamicdata.kernel.subscribeBy
import dynamicdata.list.internal.FilterStatic
import dynamicdata.list.internal.ReaderWriter
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject

class SourceList<T>(source: Observable<IChangeSet<T>>? = null) : ISourceList<T> {
    private val changes = PublishSubject.create<IChangeSet<T>>()
    private val changesPreview = PublishSubject.create<IChangeSet<T>>()
    private val _sizeChanged = lazy { PublishSubject.create<Int>() }
    private val readerWriter = ReaderWriter<T>()
    private var editLevel = 0
    private val cleanup: Disposable
    private var disposed = false
    private val lock = Any()

    init {
        val loader =
            if (source == null)
                Disposable.empty()
            else
                source.serialize()
                    .doFinally { onCompleted() }
                    .map { readerWriter.write(it) }
                    .subscribe({ invokeNext(it) }, { onError(it) }, { onCompleted() })

        cleanup = Disposable.fromAction {
            loader.dispose()
            onCompleted()
            if (_sizeChanged.isInitialized()) _sizeChanged.value.onComplete()
        }
    }

    override fun edit(action: (IExtendedList<T>) -> Unit) =
        synchronized(lock) {
            var change = ChangeSet.empty<T>()
            editLevel++

            if (editLevel == 1) {
                change = if (changesPreview.hasObservers())
                    readerWriter.writeWithPreview(action, ::invokeNextPreview)
                else
                    readerWriter.write(action)
            } else {
                readerWriter.writeNested(action)
            }

            editLevel--

            if (editLevel == 0)
                invokeNext(change)
        }

    private fun invokeNextPreview(change: IChangeSet<T>) {
        if (change.size == 0)
            return

        synchronized(lock) {
            changesPreview.onNext(change)
        }
    }

    private fun invokeNext(change: IChangeSet<T>) {
        if (change.size == 0)
            return

        synchronized(lock) {
            changes.onNext(change)
            if (_sizeChanged.isInitialized()) {
                _sizeChanged.value.onNext(readerWriter.size)
            }
        }
    }

    private fun onError(e: Throwable) {
        synchronized(lock) {
            changesPreview.onError(e)
            changes.onError(e)
        }
    }

    private fun onCompleted() {
        synchronized(lock) {
            changesPreview.onComplete()
            changes.onComplete()
        }
    }

    override fun dispose() {
        if (disposed) {
            return
        }

        synchronized(lock) {
            if (disposed) {
                return
            }
            disposed = true
        }

        cleanup.dispose()
        //changesPreview.dispose()
    }

    override fun isDisposed(): Boolean {
        return disposed
    }

    override fun connect(predicate: ((T) -> Boolean)?): Observable<IChangeSet<T>> {
        var observable = Observable.create<IChangeSet<T>> {
            synchronized(lock) {
                if (readerWriter.items.isNotEmpty()) {
                    it.onNext(ChangeSet(listOf(Change(ListChangeReason.AddRange, readerWriter.items))))
                }

                val source = changes.doFinally(it::onComplete)
                source.subscribeBy(it)
            }
        }

        if (predicate != null) {
            observable = FilterStatic(observable, predicate).run()
        }

        return observable
    }

    override fun preview(predicate: ((T) -> Boolean)?): Observable<IChangeSet<T>> {
        var observable: Observable<IChangeSet<T>> = changesPreview
        if (predicate != null)
            observable = FilterStatic(observable, predicate).run()

        return observable
    }

    override val sizeChanged: Observable<Int>
        get() = Observable.unsafeCreate {
            synchronized(lock) {
                val source = _sizeChanged.value.startWithItem(readerWriter.size).distinctUntilChanged()
                source.safeSubscribe(it)
            }
        }
    override val items: Iterable<T>
        get() = readerWriter.items

    override val size: Int
        get() = readerWriter.size

    override operator fun iterator(): Iterator<T> =
        items.iterator()
}
