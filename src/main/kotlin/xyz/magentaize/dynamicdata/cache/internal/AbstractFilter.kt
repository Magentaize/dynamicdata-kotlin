package xyz.magentaize.dynamicdata.cache.internal

import xyz.magentaize.dynamicdata.cache.*
import xyz.magentaize.dynamicdata.kernel.Optional

internal abstract class AbstractFilter<K, V>(
    private val _cache: ChangeAwareCache<K, V>,
    override val filter: (V) -> Boolean = { _ -> true }
) : Filterable<K, V> {

    override fun refresh(items: Iterable<Map.Entry<K, V>>): ChangeSet<K, V> {
        // this is an internal method only so we can be sure there are no duplicate keys in the result
        // (therefore safe to parallelise)
        fun factory(kv: Map.Entry<K, V>): Optional<Change<K, V>> {
            val existed = _cache.lookup(kv.key)
            val match = filter(kv.value)

            if (match) {
                if (!existed.hasValue) {
                    return Optional.of(Change(ChangeReason.Add, kv.key, kv.value))
                }
            } else {
                if (existed.hasValue) {
                    return Optional.of(Change(ChangeReason.Remove, kv.key, kv.value, existed))
                }
            }

            return Optional.empty()
        }

        val result = refresh(items, ::factory)
        _cache.clone(AnonymousChangeSet(result))

        return _cache.captureChanges()
    }

    override fun update(updates: ChangeSet<K, V>): ChangeSet<K, V> {
        val withFilter = getChangesWithFilter(updates)
        return processResult(withFilter)
    }

    private fun processResult(source: Iterable<UpdateWithFilter<K, V>>): ChangeSet<K, V> {
        // Have to process one item at a time as an item can be included multiple
        // times in any batch
        source.forEach { item ->
            val match = item.isMatch
            val key = item.change.key
            val u = item.change

            when (item.change.reason) {
                ChangeReason.Add -> if (match) _cache.addOrUpdate(u.current, u.key)
                ChangeReason.Update -> if (match) _cache.addOrUpdate(u.current, u.key) else _cache.remove(u.key)
                ChangeReason.Remove -> _cache.remove(u.key)
                ChangeReason.Refresh -> {
                    val existing = _cache.lookup(key)
                    if (match)
                        if (!existing.hasValue)
                            _cache.addOrUpdate(u.current, u.key)
                        else
                            _cache.refresh()
                    else
                        if (existing.hasValue)
                            _cache.remove(u.key)
                }
            }
        }

        return _cache.captureChanges()
    }

    protected abstract fun getChangesWithFilter(updates: ChangeSet<K, V>): Iterable<UpdateWithFilter<K, V>>

    protected abstract fun refresh(items: Iterable<Map.Entry<K, V>>, factory: (Map.Entry<K, V>) -> Optional<Change<K, V>>) : Collection<Change<K, V>>

    protected data class UpdateWithFilter<K, V>(val isMatch: Boolean, val change: Change<K, V>)

}
