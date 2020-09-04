package dynamicdata.list.internal

import dynamicdata.cache.internal.CombineOperator
import dynamicdata.list.ChangeAwareListWithRefCounts
import dynamicdata.list.IChangeSet
import dynamicdata.list.ListChangeReason
import dynamicdata.list.flatten
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable

internal class Combiner<T>(
    private val _source: Collection<Observable<IChangeSet<T>>>,
    private val _type: CombineOperator
) {
    private val _lock = Any()

    fun run(): Observable<IChangeSet<T>> =
        Observable.unsafeCreate { observer ->
            val disposable = CompositeDisposable()
            val resultList = ChangeAwareListWithRefCounts<T>()

            synchronized(_lock) {
                val sourceLists = List(_source.size) { ReferenceCountTracker<T>() }

                _source.zip(sourceLists).forEach { (item, list) ->
                    disposable.add(item.serialize()
                        .subscribe { changes ->
                            cloneSourceList(list, changes)

                            val notifications = updateResultList(changes, sourceLists, resultList)
                            if (notifications.size != 0)
                                observer.onNext(notifications)
                        })
                }
            }
        }

    private fun cloneSourceList(tracker: ReferenceCountTracker<T>, changes: IChangeSet<T>) =
        changes.forEach {
            when (it.reason) {
                ListChangeReason.Add -> tracker.add(it.item.current)
                ListChangeReason.AddRange -> it.range.forEach { t -> tracker.add(t) }
                ListChangeReason.Replace -> {
                    tracker.remove(it.item.previous!!)
                    tracker.add(it.item.current)
                }
                ListChangeReason.Remove -> tracker.remove(it.item.current)
                in setOf(ListChangeReason.RemoveRange, ListChangeReason.Clear) ->
                    it.range.forEach { t -> tracker.remove(t) }
            }
        }

    private fun updateResultList(
        changes: IChangeSet<T>,
        sourceLists: List<ReferenceCountTracker<T>>,
        resultList: ChangeAwareListWithRefCounts<T>
    ): IChangeSet<T> {
        changes.flatten().forEach { change ->
            when (change.reason) {
                in setOf(ListChangeReason.Add, ListChangeReason.Remove) ->
                    updateItemMembership(change.current, sourceLists, resultList)
                ListChangeReason.Replace -> {
                    updateItemMembership(change.previous!!, sourceLists, resultList)
                    updateItemMembership(change.current, sourceLists, resultList)
                }
                // Pass through refresh changes:
                ListChangeReason.Refresh ->
                    resultList.refresh(change.current)
                // A move does not affect contents and so can be ignored:
                ListChangeReason.Moved -> {
                }
                // These should not occur as they are replaced by the Flatten operator:
                //case ListChangeReason.AddRange:
                //case ListChangeReason.RemoveRange:
                //case ListChangeReason.Clear:
                else -> throw IllegalArgumentException("Unsupported change type.")
            }
        }

        return resultList.captureChanges()
    }

    private fun updateItemMembership(
        item: T,
        sourceLists: List<ReferenceCountTracker<T>>,
        resultList: ChangeAwareListWithRefCounts<T>
    ) {
        val isInResult = resultList.contains(item);
        val shouldBeInResult = matchesConstraint(sourceLists, item);
        if (shouldBeInResult && !isInResult) {
            resultList.add(item);
        } else if (!shouldBeInResult && isInResult) {
            resultList.remove(item);
        }
    }

    private fun matchesConstraint(sourceLists: List<ReferenceCountTracker<T>>, item: T): Boolean =
        when (_type) {
            CombineOperator.And -> sourceLists.all { it.contains(item) }
            CombineOperator.Or -> sourceLists.any { it.contains(item) }
            CombineOperator.Xor -> sourceLists.count { it.contains(item) } == 1
            CombineOperator.Except -> {
                val first = sourceLists[0].contains(item)
                val others = sourceLists.drop(1).any { it.contains(item) }
                first && !others
            }
            else -> throw IllegalArgumentException("item")
        }
}
