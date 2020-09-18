package dynamicdata.list

import dynamicdata.cache.PageResponse

interface PageChangeSet<T> : IChangeSet<T> {
    val response: PageResponse
}
