package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.kernel.*
import xyz.magentaize.dynamicdata.kernel.subscribeBy
import xyz.magentaize.dynamicdata.list.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.internal.functions.Functions

internal class GroupOnMutable<T, K>(
    private val _source: Observable<ChangeSet<T>>,
    private val _selector: (T) -> K,
    private val _regroup: Observable<Unit>
) {
    fun run(): Observable<ChangeSet<MutableGroup<T, K>>> =
        ObservableEx.create { emitter ->
            val groupings = ChangeAwareList<MutableGroup<T, K>>()
            val groupCache = mutableMapOf<K, AnonymousMutableGroup<T, K>>()

            //capture the grouping up front which has the benefit that the group key is only selected once
            val itemWithGroup = _source
                .transformWithOptional({ t, prev: Optional<ItemWithGroupKey<T, K>> ->
                    ItemWithGroupKey(t, _selector(t), prev.convert { it.group })
                }, true)

            val shared = itemWithGroup.publish()

            val group = shared.map { changes -> process(groupings, groupCache, changes) }

            val regroup = Observable.combineLatest(_regroup, shared.toCollection()) { _, list ->
                regroup(groupings, groupCache, list)
            }

            val publisher = group.mergeWith(regroup)
                .disposeMany()
                .notEmpty()
                .subscribeBy(emitter)

            return@create CompositeDisposable(
                publisher,
                shared.connect()
            )
        }

    private fun process(
        result: ChangeAwareList<MutableGroup<T, K>>,
        groupCache: MutableMap<K, AnonymousMutableGroup<T, K>>,
        changes: ChangeSet<ItemWithGroupKey<T, K>>
    ): ChangeSet<MutableGroup<T, K>> {
        changes.unified().groupBy { it.current.group }.forEach { grouping ->
            //lookup group and if created, add to result set
            val currentGroup = grouping.key
            val lookup = getCache(groupCache, currentGroup)
            val cache = lookup.group

            if (lookup.wasCreated)
                result.add(cache)

            //start a group edit session, so all changes are batched
            cache.edit { list ->
                grouping.value.forEach {
                    when (it.reason) {
                        ListChangeReason.Add ->
                            list.add(it.current.item)

                        ListChangeReason.Replace -> {
                            val prevItem = it.previous.value.item
                            val prevGroup = it.previous.value.group

                            //check whether an item changing has resulted in a different group
                            if (prevGroup == currentGroup) {
                                val idx = list.indexOf(prevItem)
                                list[idx] = it.current.item
                            } else {
                                list.add(it.current.item)

                                removeFromOldGroup(groupCache, prevGroup, prevItem, result)
                            }
                        }

                        ListChangeReason.Refresh -> {
                            //1. Check whether item was in the group and should not be now (or vice versa)
                            val currentItem = it.current.item
                            val prevGroup = it.current.previousGroup.value

                            //check whether an item changing has resulted in a different group
                            if (prevGroup == currentGroup) {
                                // Propagate refresh event
                                val cal = list as ChangeAwareList<T>
                                cal.refresh(currentItem)
                            } else {
                                list.add(currentItem)

                                removeFromOldGroup(groupCache, prevGroup, currentItem, result)
                            }
                        }

                        ListChangeReason.Remove ->
                            list.remove(it.current.item)

                        ListChangeReason.Clear ->
                            list.clear()

                        else -> Functions.EMPTY_ACTION
                    }
                }
            }

            if (cache.list.size == 0) {
                groupCache.remove(cache.key)
                result.remove(cache)
            }
        }

        return result.captureChanges()
    }

    private fun removeFromOldGroup(
        groupCache: MutableMap<K, AnonymousMutableGroup<T, K>>,
        prevGroup: K,
        prevItem: T,
        result: ChangeAwareList<MutableGroup<T, K>>
    ) {
        groupCache.lookup(prevGroup)
            .ifHasValue { g ->
                g.edit { oldList -> oldList.remove(prevItem) }
                if (g.list.size != 0) return@ifHasValue

                groupCache.remove(g.key)
                result.remove(g)
            }
    }

    private fun regroup(
        result: ChangeAwareList<MutableGroup<T, K>>,
        groupCache: MutableMap<K, AnonymousMutableGroup<T, K>>,
        currentItems: List<ItemWithGroupKey<T, K>>
    ): ChangeSet<MutableGroup<T, K>> {
        currentItems.forEach { iwv ->
            val currentGroupKey = iwv.group
            val newGroupKey = _selector(iwv.item)
            if (newGroupKey == currentGroupKey) return@forEach

            //remove from the old group
            val currentGroupLookup = getCache(groupCache, currentGroupKey)
            val currentGroupCache = currentGroupLookup.group
            currentGroupCache.edit { it.remove(iwv.item) }

            if (currentGroupCache.list.size == 0) {
                groupCache.remove(currentGroupKey)
                result.remove(currentGroupCache)
            }

            //Mark the old item with the new cache group
            iwv.group = newGroupKey

            //add to the new group
            val newGroupLookup = getCache(groupCache, newGroupKey)
            val newGroupCache = newGroupLookup.group
            newGroupCache.edit { it.add(iwv.item) }

            if (newGroupLookup.wasCreated)
                result.add(newGroupCache)
        }

        return result.captureChanges()
    }

    private fun getCache(groupCache: MutableMap<K, AnonymousMutableGroup<T, K>>, key: K): GroupWithAddIndicator<T, K> {
        val cache = groupCache.lookup(key)

        if (cache.hasValue)
            return GroupWithAddIndicator(cache.value, false)

        val newCache = AnonymousMutableGroup<T, K>(key)
        groupCache[key] = newCache

        return GroupWithAddIndicator(newCache, true)
    }

    class GroupWithAddIndicator<T, K>(
        val group: AnonymousMutableGroup<T, K>,
        val wasCreated: Boolean
    )
}
