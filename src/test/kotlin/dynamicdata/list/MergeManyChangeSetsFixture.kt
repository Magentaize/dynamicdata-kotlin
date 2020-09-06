package dynamicdata.list

import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class MergeManyChangeSetsFixture {
    @Test
    fun mergeManyShouldWork() {
        val a = SourceList<Int>()
        val b = SourceList<Int>()
        val c = SourceList<Int>()
        val parent = SourceList<SourceList<Int>>()
        parent.add(a)
        parent.add(b)
        parent.add(c)

        val d = parent.connect()
            .mergeMany { it.connect().removeIndex() }
            .asObservableList()

        d.size shouldBeEqualTo 0

        a.add(1)
        d.size shouldBeEqualTo 1

        a.add(2)
        d.size shouldBeEqualTo 2

        b.add(3)
        d.size shouldBeEqualTo 3

        b.add(5)
        d.size shouldBeEqualTo 4
        d.items.toList() shouldBeEqualTo listOf(1, 2, 3, 5)

        b.clear()
        d.size shouldBeEqualTo 2
        d.items.toList() shouldBeEqualTo listOf(1, 2)
    }
}
