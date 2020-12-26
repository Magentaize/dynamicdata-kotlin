package xyz.magentaize.dynamicdata.cache.internal

import xyz.magentaize.dynamicdata.cache.*
import xyz.magentaize.dynamicdata.kernel.Optional
import xyz.magentaize.dynamicdata.kernel.Stub
import xyz.magentaize.dynamicdata.kernel.lookup

internal class ReaderWriter<K, V>(private val _keySelector: (V) -> K = Stub.emptyMapper()) {

    private var _data = mutableMapOf<K, V>()
    private var _activeUpdater: CacheUpdater<K, V>? = null
    private val lock = Any()

    fun write(
        collectChanges: Boolean,
        changes: ChangeSet<K, V>,
        previewHandler: (ChangeSet<K, V>) -> Unit = Stub.EMPTY_COMSUMER,
    ): ChangeSet<K, V> =
        doUpdate(collectChanges, { it.clone(changes) }, previewHandler)

    @JvmName("writeWithCacheUpdater")
    fun write(
        collectChanges: Boolean,
        updateAction: (CacheUpdater<K, V>) -> Unit,
        previewHandler: (ChangeSet<K, V>) -> Unit = Stub.EMPTY_COMSUMER,
    ): ChangeSet<K, V> =
        doUpdate(collectChanges, updateAction, previewHandler)

    @JvmName("writeWithSourceUpdater")
    fun write(
        collectChanges: Boolean,
        updateAction: (ISourceUpdater<K, V>) -> Unit,
        previewHandler: (ChangeSet<K, V>) -> Unit = Stub.EMPTY_COMSUMER,
    ): ChangeSet<K, V> =
        doUpdate(collectChanges, updateAction, previewHandler)

    private fun doUpdate(
        collectChanges: Boolean,
        updateAction: (CacheUpdater<K, V>) -> Unit,
        previewHandler: (ChangeSet<K, V>) -> Unit = Stub.EMPTY_COMSUMER
    ) =
        synchronized(lock) {
            if (previewHandler != Stub.EMPTY_COMSUMER) {
                var copy = _data.toMutableMap()
                val changeAwareCache = ChangeAwareCache(_data)

                _activeUpdater = CacheUpdater(changeAwareCache, _keySelector)
                updateAction(_activeUpdater!!)
                _activeUpdater = null

                val changes = changeAwareCache.captureChanges()

                copy = _data.also { _data = copy }
                previewHandler(changes)
                copy = _data.also { _data = copy }

                return@synchronized changes
            }

            if (collectChanges) {
                val changeAwareCache = ChangeAwareCache(_data)

                _activeUpdater = CacheUpdater(changeAwareCache, _keySelector)
                updateAction(_activeUpdater!!)
                _activeUpdater = null

                return@synchronized changeAwareCache.captureChanges()
            }

            _activeUpdater = CacheUpdater(_data, _keySelector)
            updateAction(_activeUpdater!!)
            _activeUpdater = null

            return@synchronized AnonymousChangeSet.empty()
        }

    fun writeNested(updateAction: (ISourceUpdater<K, V>) -> Unit) =
        synchronized(lock) {
            updateAction(_activeUpdater!!)
        }

    fun lookup(key: K): Optional<V> =
        synchronized(lock) {
            return@synchronized _data.lookup(key)
        }

    fun getInitialUpdates(filter: (V) -> Boolean = Stub.emptyFilter()) =
        synchronized(lock) {
            val dict = _data
            if (dict.isEmpty())
                return@synchronized AnonymousChangeSet.empty<K, V>()

            val changes =
                if (filter == Stub.emptyFilter<V>())
                    AnonymousChangeSet<K, V>(dict.size)
                else
                    AnonymousChangeSet()

            dict.forEach { (k, v) ->
                if (filter == Stub.emptyFilter<V>() || filter(v))
                    changes.add(Change(ChangeReason.Add, k, v))
            }

            return@synchronized changes
        }

    val keyValues: Map<K, V>
        get() = synchronized(lock) {
            _data.toMap()
        }

    val keys: Collection<K>
        get() = synchronized(lock) {
            _data.keys.toList()
        }

    val items: Collection<V>
        get() = synchronized(lock) {
            _data.values.toList()
        }

    val size: Int
        get() = synchronized(lock) {
            _data.size
        }
}