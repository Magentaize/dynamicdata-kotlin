package xyz.magentaize.dynamicdata.cache.test

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.internal.functions.Functions
import xyz.magentaize.dynamicdata.cache.ChangeSet
import xyz.magentaize.dynamicdata.cache.ObservableCache
import xyz.magentaize.dynamicdata.cache.asObservableCache
import xyz.magentaize.dynamicdata.diagnostics.ChangeSummary
import xyz.magentaize.dynamicdata.diagnostics.collectUpdateStats

class ChangeSetAggregator<K, V>(source: Observable<ChangeSet<K, V>>) : Disposable {
    private val _cleanUp: Disposable
    private var _isDisposed = false

    val data: ObservableCache<K, V>
    val messages = mutableListOf<ChangeSet<K, V>>()
    lateinit var error: Throwable
        private set
    var summary: ChangeSummary = ChangeSummary.empty()
        private set

    init {
        val published = source.publish()
        data = published.asObservableCache()
        val results = published.subscribe({
            messages.add(it)
        },
            { error = it }
        )
        val summariser = published
            .collectUpdateStats()
            .subscribe({ summary = it }, Functions.emptyConsumer())
        val connected = published.connect()

        _cleanUp = Disposable.fromAction {
            data.dispose()
            connected.dispose()
            summariser.dispose()
            results.dispose()
        }
    }

    override fun dispose() {
        _cleanUp.dispose()
        _isDisposed = true
    }

    override fun isDisposed(): Boolean =
        _isDisposed
}