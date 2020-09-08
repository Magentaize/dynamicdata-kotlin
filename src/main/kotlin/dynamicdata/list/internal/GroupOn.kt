package dynamicdata.list.internal

import dynamicdata.kernel.*
import dynamicdata.kernel.subscribeBy
import dynamicdata.list.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable

internal class GroupOn<T, K>(
    private val _source: Observable<IChangeSet<T>>,
    private val _selector: (T) -> K,
    private val _regroup: Observable<Unit>
) {
    fun run(): Observable<IChangeSet<Grouping<T, K>>> =
        Observable.create { emitter ->
            val groupings = ChangeAwareList<Grouping<T, K>>()
            val groupCache = mutableMapOf<K, GroupContainer<T, K>>()

            //capture the grouping up front which has the benefit that the group key is only selected once
            val itemWithGroup = _source
                .transform({ t, prev: Optional<ItemWithGroupKey<T, K>> ->
                    ItemWithGroupKey(t, _selector(t), prev.convert { it.group })
                }, true)

            val shared = itemWithGroup.publish()

            val group = shared
                .map { changes -> process(groupings, groupCache, changes) }

            val regroup = Observable
                .combineLatest(_regroup, shared.toCollection()) { _, list ->
                    regroup(groupings, groupCache, list)
                }

            val publisher = group.mergeWith(regroup)
                .disposeMany()
                .notEmpty()
                .subscribeBy(emitter)

            val d = CompositeDisposable(
                publisher,
                shared.connect()
            )

            emitter.setDisposable(d)
        }

    private fun process(
        result: ChangeAwareList<Grouping<T, K>>,
        allGroupings: MutableMap<K, GroupContainer<T, K>>,
        changes: IChangeSet<ItemWithGroupKey<T, K>>
    ): IChangeSet<Grouping<T, K>> {
        //need to keep track of effected groups to calculate correct notifications
        val initialStateOfGroups = mutableMapOf<K, Grouping<T, K>>()

        changes.unified().groupBy { it.current.group }.forEach { grouping ->
            //lookup group and if created, add to result set
            val currentGroup = grouping.key
            val container = getGroup(allGroupings, currentGroup)

            fun getInitialState() {
                if (!initialStateOfGroups.containsKey(grouping.key))
                    initialStateOfGroups[grouping.key] = getGroupState(container)
            }

            val listToModify = container.list

            grouping.value.forEach {
                when (it.reason) {
                    ListChangeReason.Add -> {
                        getInitialState()
                        listToModify.add(it.current.item)
                    }

                    ListChangeReason.Refresh -> {
                        val prevItem = it.current.item
                        val prevGroup = it.current.previousGroup.value
                        val currentItem = it.current.item

                        //check whether an item changing has resulted in a different group
                        if (prevGroup != currentGroup) {
                            getInitialState()
                            listToModify.add(currentItem)

                            allGroupings.lookup(prevGroup)
                                .ifHasValue { g ->
                                    if (!initialStateOfGroups.containsKey(g.key))
                                        initialStateOfGroups[g.key] = getGroupState(g.key, g.list)

                                    g.list.remove(prevItem)
                                }
                        }
                    }

                    ListChangeReason.Replace -> {
                        getInitialState()
                        val prevItem = it.previous.value.item
                        val prevGroup = it.previous.value.group

                        if (prevGroup == currentGroup) {
                            val idx = listToModify.indexOf(prevItem)
                            listToModify[idx] = it.current.item
                        } else {
                            listToModify.add(it.current.item)

                            allGroupings.lookup(prevGroup)
                                .ifHasValue { g ->
                                    if (!initialStateOfGroups.containsKey(g.key))
                                        initialStateOfGroups[g.key] = getGroupState(g.key, g.list)

                                    g.list.remove(prevItem)
                                }
                        }
                    }

                    ListChangeReason.Remove -> {
                        getInitialState()
                        listToModify.remove(it.current.item)
                    }

                    ListChangeReason.Clear -> {
                        getInitialState()
                        listToModify.clear()
                    }
                }
            }
        }

        return createChangeSet(result, allGroupings, initialStateOfGroups)
    }

    private fun regroup(
        result: ChangeAwareList<Grouping<T, K>>,
        allGroupings: MutableMap<K, GroupContainer<T, K>>,
        currentItems: List<ItemWithGroupKey<T, K>>
    ): IChangeSet<Grouping<T, K>> {
        val initialStateOfGroups = mutableMapOf<K, Grouping<T, K>>()

        currentItems.forEach { iwv ->
            val currentGroupKey = iwv.group
            val newGroupKey = _selector(iwv.item)
            if (newGroupKey == currentGroupKey) return@forEach

            //lookup group and if created, add to result set
            val oldGrouping = getGroup(allGroupings, currentGroupKey)
            if (!initialStateOfGroups.containsKey(currentGroupKey))
                initialStateOfGroups[currentGroupKey] = getGroupState(oldGrouping)

            //remove from the old group
            oldGrouping.list.remove(iwv.item)

            //Mark the old item with the new cache group
            iwv.group = newGroupKey

            //add to the new group
            val newGrouping = getGroup(allGroupings, newGroupKey)
            if (!initialStateOfGroups.containsKey(newGroupKey))
                initialStateOfGroups[newGroupKey] = getGroupState(newGrouping)

            newGrouping.list.add(iwv.item)
        }

        return createChangeSet(result, allGroupings, initialStateOfGroups)
    }

    private fun createChangeSet(
        result: ChangeAwareList<Grouping<T, K>>,
        allGroupings: MutableMap<K, GroupContainer<T, K>>,
        initialStateOfGroups: MutableMap<K, Grouping<T, K>>
    ): IChangeSet<Grouping<T, K>> {
        initialStateOfGroups.forEach {
            val key = it.key
            val current = allGroupings[key]!!

            if (current.list.isEmpty()) {
                allGroupings.remove(key)
                result.remove(it.value)
            } else {
                val currentState = getGroupState(current)
                if (it.value.size == 0)
                    result.add(currentState)
                else
                    result.replace(it.value, currentState)
            }
        }

        return result.captureChanges()
    }

    private fun getGroupState(grouping: GroupContainer<T, K>): Grouping<T, K> {
        return AnonymousGroup(grouping.key, grouping.list)
    }

    private fun getGroupState(key: K, list: List<T>): Grouping<T, K> {
        return AnonymousGroup(key, list)
    }

    private fun getGroup(groupCache: MutableMap<K, GroupContainer<T, K>>, key: K): GroupContainer<T, K> {
        val cached = groupCache.lookup(key)
        if (cached.hasValue)
            return cached.value

        val newCache = GroupContainer<T, K>(key)
        groupCache[key] = newCache

        return newCache
    }

    class GroupContainer<T, K>(val key: K) {
        val list: MutableList<T> = mutableListOf()
    }
}
