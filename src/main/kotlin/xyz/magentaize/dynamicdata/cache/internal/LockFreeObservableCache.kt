package xyz.magentaize.dynamicdata.cache.internal

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import xyz.magentaize.dynamicdata.cache.*
import xyz.magentaize.dynamicdata.kernel.Optional
import xyz.magentaize.dynamicdata.kernel.Stub
import xyz.magentaize.dynamicdata.kernel.subscribeBy

class LockFreeObservableCache<K, V> : ObservableCache<K, V> {
    private val _changes = PublishSubject.create<ChangeSet<K, V>>()
    private val _changesPreview = PublishSubject.create<ChangeSet<K, V>>()
    private val _sizeChanged = PublishSubject.create<Int>()
    private val _innerCache = ChangeAwareCache<K, V>()
    private val _updater: ICacheUpdater<K, V> = CacheUpdater(_innerCache)
    private val _cleanUp: Disposable
    private var _isDisposed = false

    constructor() {
        _cleanUp = Disposable.fromAction {
            _changes.onComplete()
            _changesPreview.onComplete()
        }
    }

    constructor(source: Observable<ChangeSet<K, V>>) {
        val loader = source
            .doOnEach { _innerCache.clone(it.value) }
            .map { _innerCache.captureChanges() }
            .subscribeBy(_changes)

        _cleanUp = Disposable.fromAction {
            loader.dispose()
            _changesPreview.onComplete()
            _changes.onComplete()
            _sizeChanged.onComplete()
        }
    }

    override fun connect(predicate: (V) -> Boolean): Observable<ChangeSet<K, V>> =
        Observable.defer {
            val initial = _innerCache.getInitialUpdates(predicate)
            val changes = Observable.just(initial).concatWith(_changes)

            return@defer (if (predicate == Stub.emptyFilter<V>()) changes else changes.filter(predicate))
                .notEmpty()
        }

    override fun preview(predicate: (V) -> Boolean): Observable<ChangeSet<K, V>> =
        if (predicate == Stub.emptyFilter<V>())
            _changesPreview
        else
            _changesPreview.filter(predicate)

    override fun watch(key: K): Observable<Change<K, V>> =
        Observable.create { emitter ->
            val initial = _innerCache.lookup(key)
            if (initial.hasValue)
                emitter.onNext(Change(ChangeReason.Add, key, initial.value))

            val d = _changes.subscribe { changes ->
                changes.filter { it.key == key }
                    .forEach { match ->
                        emitter.onNext(match)
                    }
            }

            emitter.setDisposable(d)
        }

    fun edit(editAction: (ICacheUpdater<K, V>) -> Unit) {
        editAction(_updater)
        _changes.onNext(_innerCache.captureChanges())
    }

    override val sizeChanged: Observable<Int>
        get() = _sizeChanged.startWithItem(_innerCache.size).distinctUntilChanged()

    override fun dispose() {
        _cleanUp.dispose()
        _isDisposed = true
    }

    override fun isDisposed(): Boolean = _isDisposed

    override val size: Int
        get() = _innerCache.size

    override val items: Iterable<V>
        get() = _innerCache.items

    override val keys: Iterable<K>
        get() = _innerCache.keys

    override val keyValues: Map<K, V>
        get() = _innerCache.keyValues

    override fun lookup(key: K): Optional<V> =
        _innerCache.lookup(key)
}