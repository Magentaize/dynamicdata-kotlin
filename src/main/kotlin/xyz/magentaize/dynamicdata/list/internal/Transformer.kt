package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.kernel.Optional
import xyz.magentaize.dynamicdata.list.*
import io.reactivex.rxjava3.core.Observable

internal class Transformer<T, R>(
    private val _source: Observable<ChangeSet<T>>,
    factory: (T, Optional<R>, Int) -> R,
    private val _transformOnRefresh: Boolean
) {
    private val _containerFactory: (T, Optional<R>, Int) -> TransformedItemContainer<T, R> =
        { item, prev, index -> TransformedItemContainer(item, factory(item, prev, index)) }

    fun run(): Observable<ChangeSet<R>> =
        _source.scan(ChangeAwareList<TransformedItemContainer<T, R>>()) { state, changes ->
            transform(state, changes)
            state
        }
            // scan operator in rxjava will emit seed as the first item,
            // so just drop it
            .skip(1)
            .map {
                val changed = it.captureChanges()
                changed.transform { container -> container.destination }
            }

    private fun transform(transformed: ChangeAwareList<TransformedItemContainer<T, R>>, changes: ChangeSet<T>) =
        changes.forEach { item ->
            val change = item.item

            when (item.reason) {
                ListChangeReason.Add -> {
                    if (change.currentIndex < 0 || change.currentIndex >= transformed.size)
                        transformed.add(_containerFactory(change.current, Optional.empty(), transformed.size))
                    else {
                        val converted = _containerFactory(change.current, Optional.empty(), change.currentIndex)
                        transformed.add(change.currentIndex, converted)
                    }
                }
                ListChangeReason.AddRange -> {
                    val startIndex = if (item.range.index < 0) transformed.size else item.range.index

                    transformed.addOrInsertRange(
                        item.range
                            .mapIndexed { idx, t -> _containerFactory(t, Optional.empty(), idx + startIndex) },
                        item.range.index
                    )
                }
                ListChangeReason.Refresh -> {
                    if (_transformOnRefresh) {
                        val previous = Optional.of(transformed[change.currentIndex].destination)
                        val refreshed = _containerFactory(change.current, previous, change.currentIndex)

                        transformed[change.currentIndex] = refreshed
                    } else {
                        transformed.refreshAt(change.currentIndex)
                    }
                }
                ListChangeReason.Replace -> {
                    val previous = Optional.of(transformed[change.previousIndex].destination)
                    if (change.currentIndex == change.previousIndex) {
                        transformed[change.currentIndex] =
                            _containerFactory(change.current, previous, change.currentIndex)
                    } else {
                        transformed.removeAt(change.previousIndex)
                        transformed.add(
                            change.currentIndex,
                            _containerFactory(change.current, Optional.empty(), change.currentIndex)
                        )
                    }
                }
                ListChangeReason.Remove -> {
                    if (change.currentIndex >= 0) {
                        transformed.removeAt(change.currentIndex)
                    } else {
                        val toRemove = transformed.firstOrNull { it.source === change.current }

                        if (toRemove != null) {
                            transformed.remove(toRemove)
                        }
                    }
                }
                ListChangeReason.RemoveRange -> {
                    if (item.range.index >= 0)
                        transformed.removeAll(item.range.index, item.range.size)
                    else {
                        val toRemove = transformed.filter { t -> item.range.any { current -> t.source === current } }
                        transformed.removeMany(toRemove)
                    }
                }
                ListChangeReason.Clear -> {
                    //i.e. need to store transformed reference so we can correctly clea
                    val toClear = Change(ListChangeReason.Clear, transformed)
                    transformed.clearOrRemoveMany(toClear)
                }
                ListChangeReason.Moved -> {
                    val hasIndex = change.currentIndex >= 0
                    if (!hasIndex)
                        throw UnspecifiedIndexException("Cannot move as an index was not specified")

                    transformed.move(change.previousIndex, change.currentIndex)
                }
            }
        }

    internal data class TransformedItemContainer<T, R>(
        val source: T,
        val destination: R
    ) {
        override fun toString(): String {
            return super.toString()
        }
    }
}
