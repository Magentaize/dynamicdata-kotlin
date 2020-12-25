package xyz.magentaize.dynamicdata.cache.internal

import xyz.magentaize.dynamicdata.cache.*
import xyz.magentaize.dynamicdata.kernel.Optional
import xyz.magentaize.dynamicdata.kernel.lookup

internal class ReaderWriter<K, V>(private val _keySelector: ((V) -> K)? = null) {

    private var _data = mapOf<K, V>()
    private var _activeUpdater: CacheUpdater<K, V>? = null
    private val lock = Any()

    fun write(
        changes: ChangeSet<K, V>,
        previewHandler: ((ChangeSet<K, V>) -> Unit)?,
        collectChanges: Boolean
    ): ChangeSet<K, V> =
        doUpdate({ it.clone(changes) }, previewHandler, collectChanges)

    @JvmName("writeWithCacheUpdater")
    fun write(
        updateAction: (CacheUpdater<K, V>) -> Unit,
        previewHandler: ((ChangeSet<K, V>) -> Unit)?,
        collectChanges: Boolean
    ): ChangeSet<K, V> =
        doUpdate(updateAction, previewHandler, collectChanges)

    @JvmName("writeWithSourceUpdater")
    fun write(
        updateAction: (ISourceUpdater<K, V>) -> Unit,
        previewHandler: ((ChangeSet<K, V>) -> Unit)?,
        collectChanges: Boolean
    ): ChangeSet<K, V> =
        doUpdate(updateAction, previewHandler, collectChanges)

    fun writeNested(updateAction: (ISourceUpdater<K, V>) -> Unit) =
        synchronized(lock) {
            updateAction(_activeUpdater!!)
        }

    fun lookup(key: K): Optional<V> =
        synchronized(lock) {
            return@synchronized _data.lookup(key)
        }

    fun getInitialUpdates(filter: ((V) -> Boolean)? = null) =
        synchronized(lock) {
            val dict = _data
            if (dict.isEmpty())
                return@synchronized AnonymousChangeSet.empty<K, V>()

            val changes = if (filter == null) AnonymousChangeSet<K, V>(dict.size) else AnonymousChangeSet()

            dict.forEach { (k, v) ->
                if (filter == null || filter(v))
                    changes.add(Change(ChangeReason.Add, k, v))
            }

            return@synchronized changes
        }

    val keyValues: Collection<Map.Entry<K, V>>
        get() = synchronized(lock) {
            _data.entries.toList()
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

    private fun doUpdate(
        updateAction: (CacheUpdater<K, V>) -> Unit,
        previewHandler: ((ChangeSet<K, V>) -> Unit)?,
        collectChanges: Boolean
    ) =
        synchronized(lock) {
            if (previewHandler != null) {
                var copy = _data.toMap()
                val changeAwareCache = ChangeAwareCache(_data)

                _activeUpdater = CacheUpdater(changeAwareCache, _keySelector!!)
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

                _activeUpdater = CacheUpdater(changeAwareCache, _keySelector!!)
                updateAction(_activeUpdater!!)
                _activeUpdater = null

                return@synchronized changeAwareCache.captureChanges()
            }

            _activeUpdater = CacheUpdater(_data, _keySelector!!)
            updateAction(_activeUpdater!!)
            _activeUpdater = null

            return@synchronized AnonymousChangeSet.empty()
        }
}