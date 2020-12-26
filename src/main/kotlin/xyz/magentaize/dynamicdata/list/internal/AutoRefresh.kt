package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.kernel.indexOfMany
import xyz.magentaize.dynamicdata.list.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import xyz.magentaize.dynamicdata.kernel.ObservableEx
import xyz.magentaize.dynamicdata.kernel.buffer
import kotlin.time.Duration

internal class AutoRefresh<T, R>(
    private val _source: Observable<ChangeSet<T>>,
    private val _evaluator: (T) -> Observable<R>,
    private val _duration: Duration = Duration.ZERO,
    private val _scheduler: Scheduler = Schedulers.computation()
) {
    fun run(): Observable<ChangeSet<T>> =
        ObservableEx.create { emitter ->
            val allItems = mutableListOf<T>()
            val shared = _source.serialize()
                //clone all items so we can look up the index when a change has been made
                .clone(allItems)
                .publish()

            //monitor each item observable and create change
            val itemHasChanged = shared.mergeMany { t ->
                _evaluator(t)
                    .map { t }
            }

            //create a change set, either buffered or one item at the time
            val itemsChanged = if (_duration == Duration.ZERO)
                itemHasChanged.map { listOf(it) }
            else {
                itemHasChanged.buffer(_duration, _scheduler)
                    .filter { it.any() }
            }

            val requiresRefresh = itemsChanged
                .serialize()
                .map {
                    //catch all the indices of items which have been refreshed
                    allItems.indexOfMany(it) { idx, t ->
                        Change(ListChangeReason.Refresh, t, idx)
                    }
                }
                .map { AnonymousChangeSet(it) }

            //publish refreshes and underlying changes
            val publisher = shared
                .mergeWith(requiresRefresh)
                .subscribe(emitter::onNext, emitter::onError, emitter::onComplete)

            return@create CompositeDisposable(publisher, shared.connect())
        }
}
