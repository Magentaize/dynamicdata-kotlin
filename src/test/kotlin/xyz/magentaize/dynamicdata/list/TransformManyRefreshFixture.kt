package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.domain.PersonWithFriends
import xyz.magentaize.dynamicdata.list.test.asAggregator
import xyz.magentaize.dynamicdata.utilities.recursiveMap
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

class TransformManyRefreshFixture {
    val source = SourceList<PersonWithFriends>()
    val result = source.connect()
        .autoRefresh()
        .transformMany { it.friends.recursiveMap { r -> r.friends } }
        .asAggregator()

    @Test
    fun autoRefresh() {
        val f1 = PersonWithFriends("Friend1", 40)
        val f2 = PersonWithFriends("Friend2", 45)
        val p = PersonWithFriends("Person", 50)
        source.add(p)
        p.friends = listOf(f1, f2)

        result.data.size shouldBeEqualTo 2
        result.data.items.toList() shouldBeEqualTo listOf(f1, f2)
    }

    @Test
    fun autoRefreshOnOtherProperty() {
        val f1 = PersonWithFriends("Friend1", 40)
        val f2 = PersonWithFriends("Friend2", 45)
        val fs = mutableListOf(f1)
        val p = PersonWithFriends("Person", 50, fs)
        source.add(p)
        fs.add(f2)
        p.age = 55

        result.data.size shouldBeEqualTo 2
        result.data.items.toList() shouldBeEqualTo listOf(f1, f2)
    }

    @Test
    fun autoRefreshRecursive() {
        val f1 = PersonWithFriends("Friend1", 30)
        val f2 = PersonWithFriends("Friend2", 35)
        val f3 = PersonWithFriends("Friend3", 40, listOf(f1))
        val f4 = PersonWithFriends("Friend4", 45, listOf(f2))
        val p = PersonWithFriends("Person", 50, listOf(f3))
        source.add(p)
        p.friends = listOf(f4)

        result.data.size shouldBeEqualTo 2
        result.data.items.toSet() shouldBeEqualTo setOf(f2, f4)
    }
}
