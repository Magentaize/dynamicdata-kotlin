package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.cache.PageResponse
import xyz.magentaize.dynamicdata.list.ChangeSet
import xyz.magentaize.dynamicdata.list.PageChangeSet

internal class AnonymousPageChangeSet<T>(
    private val _virtualChangeSet: ChangeSet<T>,
    override val response: PageResponse
): PageChangeSet<T>, ChangeSet<T> by _virtualChangeSet
