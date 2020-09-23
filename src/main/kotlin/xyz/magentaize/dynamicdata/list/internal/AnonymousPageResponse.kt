package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.cache.PageResponse

internal data class AnonymousPageResponse(
    override val pageSize: Int,
    override val page: Int,
    override val pages: Int,
    override val totalSize: Int
) : PageResponse
