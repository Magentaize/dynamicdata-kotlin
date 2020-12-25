package xyz.magentaize.dynamicdata.cache.internal

import io.reactivex.rxjava3.core.Observable
import xyz.magentaize.dynamicdata.cache.ChangeAwareCache
import xyz.magentaize.dynamicdata.cache.ChangeReason
import xyz.magentaize.dynamicdata.cache.ChangeSet
import xyz.magentaize.dynamicdata.cache.notEmpty
import xyz.magentaize.dynamicdata.kernel.Error
import xyz.magentaize.dynamicdata.kernel.Optional
import xyz.magentaize.dynamicdata.kernel.Stub
import java.lang.Exception

internal class Transformer<K, E, R>(
    private val _source: Observable<ChangeSet<K, E>>,
    private val _factory: (K, E, Optional<E>) -> R,
    private val _exceptionCallback: (Error<K, E>) -> Unit = (Stub)::EMPTY_CALLBACK,
    private val _transformOnRefresh: Boolean = false
) {
    fun run(): Observable<ChangeSet<K, R>> =
        _source.scan(null as ChangeAwareCache<K, R>?) { state, changes ->
            val cache = state ?: ChangeAwareCache(changes.size)

            changes.forEach { change ->
                val action = {
                    val transformed = _factory(change.key, change.current, change.previous)
                    cache.addOrUpdate(transformed, change.key)
                }

                when (change.reason) {
                    ChangeReason.Add, ChangeReason.Update -> {
                        if (_exceptionCallback != ((Stub)::EMPTY_CALLBACK)) {
                            try {
                                action()
                            } catch (ex: Exception) {
                                _exceptionCallback(Error(ex, change.key, change.current))
                            }
                        } else
                            action()
                    }

                    ChangeReason.Remove ->
                        cache.remove(change.key)

                    ChangeReason.Refresh -> {
                        if (_transformOnRefresh)
                            action()
                        else
                            cache.refresh(change.key)
                    }

                    ChangeReason.Moved ->
                        return@forEach
                }
            }

            return@scan cache
        }
            .filter { it != null }
            .map { it!!.captureChanges() }
            .notEmpty()
}

