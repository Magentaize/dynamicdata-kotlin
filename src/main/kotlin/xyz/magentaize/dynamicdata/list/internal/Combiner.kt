package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.cache.internal.CombineOperator
import xyz.magentaize.dynamicdata.list.ChangeAwareListWithRefCounts
import xyz.magentaize.dynamicdata.list.ChangeSet
import xyz.magentaize.dynamicdata.list.ListChangeReason
import xyz.magentaize.dynamicdata.list.flatten
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.internal.functions.Functions

internal class Combiner<T>(
    private val _source: Collection<Observable<ChangeSet<T>>>,
    private val _type: CombineOperator
) {
    private val _lock = Any()

    fun run(): Observable<ChangeSet<T>> =
        Observable.create { emitter ->
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
                                emitter.onNext(notifications)
                        })
                }
            }

            emitter.setDisposable(disposable)
        }

    private fun cloneSourceList(tracker: ReferenceCountTracker<T>, changes: ChangeSet<T>) =
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

                else -> Functions.EMPTY_ACTION
            }
        }

    private fun updateResultList(
        changes: ChangeSet<T>,
        sourceLists: List<ReferenceCountTracker<T>>,
        resultList: ChangeAwareListWithRefCounts<T>
    ): ChangeSet<T> {
        changes.flatten().forEach { change ->
            when (change.reason) {
                ListChangeReason.Add, ListChangeReason.Remove ->
                    updateItemMembership(change.current, sourceLists, resultList)

                ListChangeReason.Replace -> {
                    updateItemMembership(change.previous.value, sourceLists, resultList)
                    updateItemMembership(change.current, sourceLists, resultList)
                }

                // Pass through refresh changes:
                ListChangeReason.Refresh ->
                    resultList.refresh(change.current)

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

    private fun updateItemMembership(
        item: T,
        sourceLists: List<ReferenceCountTracker<T>>,
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
