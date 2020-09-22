package dynamicdata.list

import dynamicdata.cache.PageResponse

interface PageChangeSet<T> : ChangeSet<T> {
    val response: PageResponse
}
