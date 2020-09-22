package dynamicdata.list.internal

import dynamicdata.cache.internal.CombineOperator
import dynamicdata.list.*
import dynamicdata.list.ChangeAwareListWithRefCounts
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable

internal class DynamicCombiner<T>(
    private val _source: ObservableList<Observable<ChangeSet<T>>>,
    private val _type: CombineOperator
) {
    private val _lock = Any()

    fun run(): Observable<ChangeSet<T>> =
        Observable.create { emitter ->
            //this is the resulting list which produces all notifications
            val resultList = ChangeAwareListWithRefCounts<T>()

            //Transform to a merge container.
            //This populates a RefTracker when the original source is subscribed to
            val sourceLists = _source.connect()
                .serialize()
                .transform { it:Observable<ChangeSet<T>> -> MergeContainer(it) }
                .asObservableList()

            //merge the items back together
            val allChanges = sourceLists.connect()
                .mergeMany { it.source }
                .serialize()
                .subscribe { changes ->
                    //Populate result list and check for changes
                    val notifications = updateResultList(sourceLists.items.toList(), resultList, changes)
                    if (notifications.size != 0) {
                        emitter.onNext(notifications)
                    }
                }

            //When a list is removed, update all items that were in that list
            val removedItem = sourceLists.connect()
                .onItemRemoved { mc ->
                    //Remove items if required
                    val notifications =
                        updateItemSetMemberships(sourceLists.items.toList(), resultList, mc.tracker.items)
                    if (notifications.size != 0) {
                        emitter.onNext(notifications)
                    }

                    //On some operators, items not in the removed list can also be affected.
                    if (_type == CombineOperator.And || _type == CombineOperator.Except) {
                        val itemsToCheck = sourceLists.items.flatMap { mc2 -> mc2.tracker.items }.toList()
                        val notification2 =
                            updateItemSetMemberships(sourceLists.items.toList(), resultList, itemsToCheck)
                        if (notification2.size != 0) {
                            emitter.onNext(notification2)
                        }
                    }
                }
                .subscribe()

            //When a list is added, update all items that are in that list
            val sourceChanged = sourceLists.connect()
                .whereReasonsAre(ListChangeReason.Add, ListChangeReason.AddRange)
                .forEachItemChange { mc ->
                    val notifications =
                        updateItemSetMemberships(sourceLists.items.toList(), resultList, mc.current.tracker.items)
                    if (notifications.size != 0) {
                        emitter.onNext(notifications)
                    }

                    //On some operators, items not in the new list can also be affected.
                    if (_type == CombineOperator.And || _type == CombineOperator.Except) {
                        val notification2 =
                            updateItemSetMemberships(sourceLists.items.toList(), resultList, resultList.toList())
                        if (notification2.size != 0) {
                            emitter.onNext(notification2)
                        }
                    }
                }
                .subscribe()

            val d = CompositeDisposable(
                sourceLists,
                allChanges,
                removedItem,
                sourceChanged
            )

            emitter.setDisposable(d)
        }

    private fun updateItemSetMemberships(
        sourceLists: List<MergeContainer<T>>,
        resultList: ChangeAwareListWithRefCounts<T>,
        items: Iterable<T>
    ): ChangeSet<T> {
        items.forEach { updateItemMembership(it, sourceLists, resultList) }
        return resultList.captureChanges()
    }

    private fun updateItemMembership(
        item: T,
        sourceLists: List<MergeContainer<T>>,
        resultList: ChangeAwareListWithRefCounts<T>
    ) {
        val isInResult = resultList.contains(item)
        val shouldBeInResult = matchesConstraint(sourceLists, item)
        if (shouldBeInResult && !isInResult) {
            resultList.add(item)
        } else if (!shouldBeInResult && isInResult) {
            resultList.remove(item)
        }
    }

    private fun matchesConstraint(sourceLists: List<MergeContainer<T>>, item: T): Boolean {
        if (sourceLists.isEmpty()) return false

        return when (_type) {
            CombineOperator.And -> sourceLists.all { it.tracker.contains(item) }
            CombineOperator.Or -> sourceLists.any { it.tracker.contains(item) }
            CombineOperator.Xor -> sourceLists.count { it.tracker.contains(item) } == 1
            CombineOperator.Except -> {
                val first = sourceLists[0].tracker.contains(item)
                val others = sourceLists.drop(1).any { it.tracker.contains(item) }
                first && !others
            }
            else -> throw IllegalArgumentException("Unknown CombineOperator $_type")
        }
    }

    private fun updateResultList(
        sourceLists: List<MergeContainer<T>>,
        resultList: ChangeAwareListWithRefCounts<T>,
        changes: ChangeSet<T>
    ): ChangeSet<T> {
        //child caches have been updated before we reached this point.
        changes.flatten().forEach {
            when (it.reason) {
                ListChangeReason.Add, ListChangeReason.Remove ->
                    updateItemMembership(it.current, sourceLists, resultList)

                ListChangeReason.Replace -> {
                    updateItemMembership(it.previous.value, sourceLists, resultList)
                    updateItemMembership(it.current, sourceLists, resultList)
                }

                // Pass through refresh changes:
                ListChangeReason.Refresh ->
                    resultList.refresh(it.current)

                // A move does not affect contents and so can be ignored:
                ListChangeReason.Moved ->
                    return@forEach

                // These should not occur as they are replaced by the Flatten operator:
                //case ListChangeReason.AddRange:
                //case ListChangeReason.RemoveRange:
                //case ListChangeReason.Clear:
                else -> throw IllegalArgumentException("Unsupported change type.")
            }
        }

        return resultList.captureChanges()
    }

    private class MergeContainer<T>(source: Observable<ChangeSet<T>>) {
        val tracker = ReferenceCountTracker<T>()
        val source: Observable<ChangeSet<T>> = source.doOnEach { clone(it.value) }

        private fun clone(changes: ChangeSet<T>) =
            changes.forEach {
                when (it.reason) {
                    ListChangeReason.Add ->
                        tracker.add(it.item.current)

                    ListChangeReason.AddRange ->
                        it.range.forEach { t -> tracker.add(t) }

                    ListChangeReason.Replace -> {
                        tracker.remove(it.item.previous.value)
                        tracker.add(it.item.current)
                    }

                    ListChangeReason.Remove ->
                        tracker.remove(it.item.current)

                    ListChangeReason.RemoveRange, ListChangeReason.Clear ->
                        it.range.forEach { t -> tracker.remove(t) }
                }
            }
    }
}
