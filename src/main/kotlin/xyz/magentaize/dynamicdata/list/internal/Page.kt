package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.cache.AnonymousPageRequest
import xyz.magentaize.dynamicdata.cache.PageRequest
import xyz.magentaize.dynamicdata.kernel.subscribeBy
import xyz.magentaize.dynamicdata.list.*
import io.reactivex.rxjava3.core.Observable
import xyz.magentaize.dynamicdata.kernel.ObservableEx
import java.lang.Integer.min

internal class Page<T>(
    private val _source: Observable<ChangeSet<T>>,
    private val _requests: Observable<PageRequest>
) {
    @Suppress("UNCHECKED_CAST")
    fun run(): Observable<ChangeSet<T>> =
        ObservableEx.create<PageChangeSet<T>> { emitter ->
            val all = mutableListOf<T>()
            val paged = ChangeAwareList<T>()

            var parameters: PageRequest = AnonymousPageRequest(0, 25)

            val requestStream = _requests
                .doOnEach { parameters = it.value }
                .filter { !(it.page < 0 || it.size < 1) }
                .map { page(all, paged, it) }

            val dataChanged = _source
                .map { page(all, paged, parameters, it) }

            return@create requestStream.mergeWith(dataChanged)
                .filter { it.size != 0 }
                .subscribeBy(emitter)
        } as Observable<ChangeSet<T>>

    private fun page(
        all: MutableList<T>,
        paged: ChangeAwareList<T>,
        request: PageRequest,
        changeSet: ChangeSet<T>? = null
    ): PageChangeSet<T> {
        if (changeSet != null)
            all.clone(changeSet)

        val pages = calculatePages(all, request)
        val page = min(request.page, pages)
        val skip = request.size * (page - 1)

        val current = all.drop(skip)
            .take(request.size)
            .toList()

        val adds = current.minus(paged)
        val removes = paged.minus(current)

        paged.removeMany(removes)

        adds.forEach {
            val index = current.indexOf(it)
            paged.add(index, it)
        }

        val moves = (changeSet ?: emptyList()).filter {
            it.reason == ListChangeReason.Moved && it.movedWithinRange(
                skip,
                skip + request.size
            )
        }
        moves.forEach {
            val curr = it.item.currentIndex - skip
            val prev = it.item.previousIndex - skip
            paged.move(prev, curr)
        }

        //find replaces [Is this ever the case that it can be reached]
        (current.indices).forEach { i ->
            val curr = current[i]
            val prev = current[i]
            if (curr === prev) return@forEach

            val idx = paged.indexOf(curr)
            paged.move(i, idx)
        }

        val changed = paged.captureChanges()

        return AnonymousPageChangeSet(changed, AnonymousPageResponse(paged.size, page, all.size, pages))
    }

    private fun calculatePages(all: MutableList<T>, request: PageRequest): Int {
        if (request.size >= all.size || request.size == 0)
            return 1

        val pages = all.size / request.size
        val overlap = all.size % request.size

        return if (overlap == 0) pages else pages + 1
    }
}
