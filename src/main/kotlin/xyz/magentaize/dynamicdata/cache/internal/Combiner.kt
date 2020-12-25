package xyz.magentaize.dynamicdata.cache.internal

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import xyz.magentaize.dynamicdata.cache.ChangeAwareCache
import xyz.magentaize.dynamicdata.cache.ChangeReason
import xyz.magentaize.dynamicdata.cache.ChangeSet
import xyz.magentaize.dynamicdata.kernel.mapValues

internal class Combiner<K, V>(
    private val _type: CombineOperator,
    private val _updatedCallback: (ChangeSet<K, V>) -> Unit
) {
    private val _sourceCaches = mutableListOf<Cache<K, V>>()
    private val _combinedCache = ChangeAwareCache<K, V>()
    private val _lock = Any()

    fun run(source: List<Observable<ChangeSet<K, V>>>): Disposable {
        val d = CompositeDisposable()
        synchronized(_lock) {
            val caches = (0..source.size).map { Cache<K, V>() }
            _sourceCaches.addAll(caches)

            source.zip(_sourceCaches) { item, cache ->
                val sub = item.subscribe { update(cache, it) }
                d.add(sub)
            }
        }

        return d
    }

    fun update(cache: Cache<K, V>, updates: ChangeSet<K, V>) {
        val notifications: ChangeSet<K, V>
        synchronized(_lock) {
            cache.clone(updates)
            notifications = updateCombined(updates)
        }
        if (notifications.size != 0) {
            _updatedCallback(notifications)
        }
    }

    fun updateCombined(updates: ChangeSet<K, V>): ChangeSet<K, V> {
        updates.forEach { update ->
            val key = update.key
            when (update.reason) {
                ChangeReason.Add, ChangeReason.Update -> {
                    val cached = _combinedCache.lookup(key)
                    val contained = cached.hasValue
                    val match = matchesConstraint(key)

                    if (match) {
                        if (contained) {
                            if (update.current !== cached) {
                                _combinedCache.addOrUpdate(update.current, key)
                            }
                        } else {
                            _combinedCache.addOrUpdate(update.current, key)
                        }
                    } else {
                        if (contained) {
                            _combinedCache.remove(key)
                        }
                    }
                }

                ChangeReason.Remove -> {
                    val cached = _combinedCache.lookup(key)
                    val contained = cached.hasValue
                    val shouldBeIncluded = matchesConstraint(key)

                    if (shouldBeIncluded) {
                        val first = _sourceCaches.map { it.lookup(key) }
                            .mapValues()
                            .first()

                        if (!cached.hasValue) {
                            _combinedCache.addOrUpdate(first, key)
                        } else if (first !== cached.value) {
                            _combinedCache.addOrUpdate(first, key)
                        }
                    } else {
                        if (contained) {
                            _combinedCache.remove(key)
                        }
                    }
                }

                ChangeReason.Refresh ->
                    _combinedCache.refresh(key)
            }
        }

        return _combinedCache.captureChanges()
    }

    private fun matchesConstraint(key: K): Boolean =
        when (_type) {
            CombineOperator.And ->
                _sourceCaches.all { it.lookup(key).hasValue }

            CombineOperator.Or ->
                _sourceCaches.any { it.lookup(key).hasValue }

            CombineOperator.Xor ->
                _sourceCaches.count { it.lookup(key).hasValue } == 1

            CombineOperator.Except -> {
                val first = _sourceCaches.take(1).any { it.lookup(key).hasValue }
                val others = _sourceCaches.drop(1).any { it.lookup(key).hasValue }
                first && !others
            }
        }
}
