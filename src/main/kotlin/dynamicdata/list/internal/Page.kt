package dynamicdata.list.internal

import dynamicdata.cache.AnonymousPageRequest
import dynamicdata.cache.PageRequest
import dynamicdata.kernel.subscribeBy
import dynamicdata.list.*
import io.reactivex.rxjava3.core.Observable
import java.lang.Integer.min

internal class Page<T>(
    private val _source: Observable<IChangeSet<T>>,
    private val _requests: Observable<PageRequest>
) {
    fun run(): Observable<IChangeSet<T>> =
        Observable.create<PageChangeSet<T>> { emitter ->
            val all = mutableListOf<T>()
            val paged = ChangeAwareList<T>()

            var parameters: PageRequest = AnonymousPageRequest(0, 25)

            val requestStream = _requests
                .doOnEach { parameters = it.value }
                .filter { !(it.page < 0 || it.size < 1) }
                .map { page(all, paged, it) }

            val dataChanged = _source
                .map { page(all, paged, parameters, it) }

            val d = requestStream.mergeWith(dataChanged)
                .filter { it.size != 0 }
                .subscribeBy(emitter)

            emitter.setDisposable(d)
        } as Observable<IChangeSet<T>>

    private fun page(
        all: MutableList<T>,
        paged: ChangeAwareList<T>,
        request: PageRequest,
        changeSet: IChangeSet<T>? = null
    ): PageChangeSet<T> {
        if (changeSet != null)
            all.clone(changeSet)

        val prev = paged
        val pages = calculatePages(all, request)
        val page = min(request.page, pages)
        val skip = request.size * (page - 1)

        val current = all.drop(skip)
            .take(request.size)
            .toList()

        val adds = current.minus(prev)
        val removes = prev.minus(current)

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
