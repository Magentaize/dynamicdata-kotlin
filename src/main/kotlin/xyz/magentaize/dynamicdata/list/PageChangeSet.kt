package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.cache.PageResponse

interface PageChangeSet<T> : ChangeSet<T> {
    val response: PageResponse
}
