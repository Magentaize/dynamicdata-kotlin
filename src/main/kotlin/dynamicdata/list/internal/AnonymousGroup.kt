package dynamicdata.list.internal

import dynamicdata.list.Group
import kotlin.jvm.internal.Intrinsics

internal class AnonymousGroup<T, K>(
    override val key: K,
    items: List<T>
) : Group<T, K> {
    override val items: List<T> = items.toList()

    override val size: Int
        get() = items.size

    override fun equals(var1: Any?): Boolean {
        return if (this !== var1) {
            if (var1 is AnonymousGroup<*, *>) {
                if (Intrinsics.areEqual(this.key, var1.key)) {
                    return true
                }
            }
            false
        } else {
            true
        }
    }

    override fun toString(): String {
        return "Grouping for: $key ($size items)"
    }
}
