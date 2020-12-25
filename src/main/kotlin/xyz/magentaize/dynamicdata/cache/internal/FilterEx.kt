package xyz.magentaize.dynamicdata.cache.internal

import xyz.magentaize.dynamicdata.cache.ChangeAwareCache
import xyz.magentaize.dynamicdata.cache.ChangeReason
import xyz.magentaize.dynamicdata.cache.ChangeSet

internal fun <K, V> ChangeAwareCache<K, V>.filterChanges(
    changes: ChangeSet<K, V>,
    predicate: (V) -> Boolean
) =
    changes.forEach { change ->
        val key = change.key
        when (change.reason) {
            ChangeReason.Add -> {
                val curr = change.current
                if (predicate(curr))
                    addOrUpdate(curr, key)
            }

            ChangeReason.Update -> {
                val curr = change.current
                if (predicate(curr))
                    addOrUpdate(curr, key)
                else
                    remove(key)
            }

            ChangeReason.Remove ->
                remove(key)

            ChangeReason.Refresh -> {
                val existing = lookup(key)
                if (predicate(change.current)) {
                    if (!existing.hasValue)
                        addOrUpdate(change.current, key)
                    else
                        refresh(key)
                } else {
                    if (existing.hasValue)
                        remove(key)
                }
            }
        }
    }