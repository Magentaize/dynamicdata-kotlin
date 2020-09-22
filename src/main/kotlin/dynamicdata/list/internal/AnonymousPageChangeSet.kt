package dynamicdata.list.internal

import dynamicdata.cache.PageResponse
import dynamicdata.list.ChangeSet
import dynamicdata.list.PageChangeSet

internal class AnonymousPageChangeSet<T>(
    private val _virtualChangeSet: ChangeSet<T>,
    override val response: PageResponse
): PageChangeSet<T>, ChangeSet<T> by _virtualChangeSet
