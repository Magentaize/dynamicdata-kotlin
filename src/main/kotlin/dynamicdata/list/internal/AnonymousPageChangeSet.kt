package dynamicdata.list.internal

import dynamicdata.cache.PageResponse
import dynamicdata.list.IChangeSet
import dynamicdata.list.PageChangeSet

internal class AnonymousPageChangeSet<T>(
    private val _virtualChangeSet: IChangeSet<T>,
    override val response: PageResponse
): PageChangeSet<T>, IChangeSet<T> by _virtualChangeSet
