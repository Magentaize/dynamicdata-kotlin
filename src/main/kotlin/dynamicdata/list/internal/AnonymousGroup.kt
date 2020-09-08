package dynamicdata.list.internal

import dynamicdata.list.Grouping

internal class AnonymousGroup<T, K>(
    override val key: K,
    override val items: List<T>
) : Grouping<T, K> {
    override val size: Int
        get() = items.size

    override fun toString(): String {
        return "Grouping for: $key ($size items)"
    }
}
