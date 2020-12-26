package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.kernel.subscribeBy
import xyz.magentaize.dynamicdata.list.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import xyz.magentaize.dynamicdata.kernel.ObservableEx
import xyz.magentaize.dynamicdata.kernel.Stub

internal class TransformMany<T, R>(
    private val _source: Observable<ChangeSet<T>>,
    private val _selector: (T) -> Iterable<R>,
    private val _childChanges: (T) -> Observable<ChangeSet<R>> = Stub.emptyMapper()
) {

    fun run(): Observable<ChangeSet<R>> {
        if (_childChanges != Stub.emptyMapper<T, Observable<ChangeSet<R>>>())
            createWithChangeset()

        return ObservableEx.create { emitter ->
            val result = ChangeAwareList<R>()

            return@create _source.transform({ ManyContainer(_selector(it).toList(), Observable.never()) }, true)
                .map {
                    val iter = DestinationEnumerator(it)
                    result.clone(iter)
                    return@map result.captureChanges()
                }
                .notEmpty()
                .subscribeBy(emitter)
        }
    }

    private fun createWithChangeset(): Observable<ChangeSet<R>> =
        Observable.create { emitter ->
            val result = ChangeAwareList<R>()

            val transformed = _source.transform {
                val list = _selector(it)
                val changes = _childChanges.invoke(it).skip(1)
                return@transform ManyContainer(list, changes)
            }.publish()

            val initial = transformed
                .map { AnonymousChangeSet(DestinationEnumerator(it).toList()) }

            val subsequent = transformed
                .mergeMany { it.changes }

            val init = initial.map {
                result.clone(it)
                return@map result.captureChanges()
            }

            val subseq = subsequent.removeIndex()
                .map {
                    result.clone(it)
                    return@map result.captureChanges()
                }

            val allChanges = init.mergeWith(subseq)

            val d = CompositeDisposable(allChanges.subscribeBy(emitter), transformed.connect())

            emitter.setDisposable(d)
        }

    private class DestinationEnumerator<R>(
        private val _changes: ChangeSet<ManyContainer<R>>
    ) : Iterable<Change<R>> {
        override fun iterator(): Iterator<Change<R>> =
            iterator {
                _changes.forEach { change ->
                    when (change.reason) {
                        ListChangeReason.Add ->
                            yieldAll(change.item.current.destination.map { Change(change.reason, it) })

                        ListChangeReason.AddRange, ListChangeReason.Clear -> {
                            val items = change.range.flatMap { it.destination }
                            yield(Change(change.reason, items))
                        }

                        ListChangeReason.Replace, ListChangeReason.Refresh -> {
                            //this is difficult as we need to discover adds and removes (and perhaps replaced)
                            val curr = change.item.current.destination.toList()
                            val prev = change.item.previous.value.destination.toList()
                            val adds = curr.minus(prev)
                            val removes = prev.minus(curr)

                            //I am not sure whether it is possible to translate the original change into a replace
                            yieldAll(removes.map { Change(ListChangeReason.Remove, it) })
                            yieldAll(adds.map { Change(ListChangeReason.Add, it) })
                        }

                        ListChangeReason.Remove ->
                            yieldAll(change.item.current.destination.map { Change(change.reason, it) })

                        ListChangeReason.RemoveRange -> {
                            val toRemove = change.range.flatMap { it.destination }
                            yieldAll(toRemove.map { Change(ListChangeReason.Remove, it) })
                        }

                        ListChangeReason.Moved ->
                            //do nothing as the original index has no bearing on the destination index
                            return@forEach
                    }
                }
            }
    }

    private class ManyContainer<R>(
        val destination: Iterable<R>,
        val changes: Observable<ChangeSet<R>>
    )
}
