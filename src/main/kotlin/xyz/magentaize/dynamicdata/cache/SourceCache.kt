package xyz.magentaize.dynamicdata.cache

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import xyz.magentaize.dynamicdata.cache.internal.ReaderWriter
import xyz.magentaize.dynamicdata.kernel.Optional
import xyz.magentaize.dynamicdata.kernel.Stub
import xyz.magentaize.dynamicdata.kernel.subscribeBy

class SourceCache<K, V> private constructor() : EditableSourceCache<K, V> {
    override lateinit var keySelector: (V) -> K
        private set
    private val _lock = Any()
    private val _changes = PublishSubject.create<ChangeSet<K, V>>()
    private val _changesPreview = PublishSubject.create<ChangeSet<K, V>>()
    private val _sizeChanged = lazy { PublishSubject.create<Int>() }
    private lateinit var _readerWriter: ReaderWriter<K, V>
    private lateinit var _cleanUp: Disposable
    private var _editLevel = 0
    private var _isDisposed = false

    constructor(_source: Observable<ChangeSet<K, V>>) : this() {
        keySelector = Stub.emptyMapper()
        _readerWriter = ReaderWriter()

        val loader = _source.doFinally {
            _changes.onComplete()
            _changesPreview.onComplete()
        }.subscribe({ changeSet ->
            val previewHandler =
                if (_changesPreview.hasObservers())
                    this::invokePreview
                else
                    Stub.EMPTY_COMSUMER
            val changes = _readerWriter.write(_changes.hasObservers(), changeSet, previewHandler)
            invokeNext(changes)
        }, { ex ->
            _changesPreview.onError(ex)
            _changes.onError(ex)
        })

        _cleanUp = Disposable.fromAction {
            loader.dispose()
            _changes.onComplete()
            _changesPreview.onComplete()
            if (_sizeChanged.isInitialized()) {
                _sizeChanged.value.onComplete()
            }
            _isDisposed = true
        }
    }

    constructor(keySelector: (V) -> K = Stub.emptyMapper()) : this() {
        this.keySelector = keySelector
        _readerWriter = ReaderWriter(keySelector)

        _cleanUp = Disposable.fromAction {
            _changes.onComplete()
            _changesPreview.onComplete()
            if (_sizeChanged.isInitialized()) {
                _sizeChanged.value.onComplete()
            }
            _isDisposed = true
        }
    }

    override fun connect(predicate: (V) -> Boolean): Observable<ChangeSet<K, V>> =
        Observable.create { emitter ->
            synchronized(_lock) {
                val initial = getInitialUpdates(predicate)
                if (initial.size != 0)
                    emitter.onNext(initial)

                val updateSource = (if (predicate == Stub.emptyFilter<V>()) _changes else _changes.filter(predicate))
                    .notEmpty()

                emitter.setDisposable(updateSource.subscribeBy(emitter))
            }
        }

    override fun preview(predicate: (V) -> Boolean): Observable<ChangeSet<K, V>> {
        return if (predicate == Stub.emptyFilter<V>())
            _changesPreview
        else
            _changesPreview.filter(predicate)
    }

    override fun watch(key: K): Observable<Change<K, V>> =
        Observable.create { emitter ->
            synchronized(_lock) {
                val initial = _readerWriter.lookup(key)
                if (initial.hasValue)
                    emitter.onNext(Change(ChangeReason.Add, key, initial.value))

                val d = _changes.doFinally(emitter::onComplete)
                    .subscribe { changes ->
                        changes.forEach {
                            if (it.key == key)
                                emitter.onNext(it)
                        }
                    }

                emitter.setDisposable(d)
            }
        }

    override fun lookup(key: K): Optional<V> =
        _readerWriter.lookup(key)

    override fun dispose() =
        _cleanUp.dispose()

    override fun isDisposed(): Boolean =
        _isDisposed

    override val sizeChanged: Observable<Int>
        get() = Observable.create { emitter ->
            synchronized(_lock) {
                val source = _sizeChanged.value.startWithItem(_readerWriter.size).distinctUntilChanged()
                emitter.setDisposable(source.subscribeBy(emitter))
            }
        }

    override val size: Int
        get() = _readerWriter.size

    override val items: Iterable<V>
        get() = _readerWriter.items

    override val keys: Iterable<K>
        get() = _readerWriter.keys

    override val keyValues: Map<K, V>
        get() = _readerWriter.keyValues

    internal fun updateFromSource(updateAction: (ISourceUpdater<K, V>) -> Unit) =
        synchronized(_lock) {
            var changes: ChangeSet<K, V>? = null

            _editLevel++;
            if (_editLevel == 1) {
                val previewHandler =
                    if (_changesPreview.hasObservers())
                        this::invokePreview
                    else
                        Stub.EMPTY_COMSUMER
                changes = _readerWriter.write(_changes.hasObservers(), updateAction, previewHandler)
            } else
                _readerWriter.writeNested(updateAction)

            _editLevel--

            if (changes != null && _editLevel == 0) {
                invokeNext(changes)
            }
        }

    private fun getInitialUpdates(filter: (V) -> Boolean) =
        _readerWriter.getInitialUpdates(filter)

    private fun invokeNext(changes: ChangeSet<K, V>) =
        synchronized(_lock) {
            if (changes.size != 0)
                _changes.onNext(changes)

            if (_sizeChanged.isInitialized()) {
                _sizeChanged.value.onNext(_readerWriter.size)
            }
        }

    private fun invokePreview(changes: ChangeSet<K, V>) =
        synchronized(_lock) {
            if (changes.size != 0)
                _changesPreview.onNext(changes)
        }

    override fun edit(updateAction: (ISourceUpdater<K, V>) -> Unit) =
        updateFromSource(updateAction)
}