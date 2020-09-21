package dynamicdata.list

import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class RemoveManyFixture {
    val list = mutableListOf<Int>()

    @Test
    fun removeManyWillRemoveARange() {
        list.addAll(1..10)
        list.removeMany(2..9)
        list.toList() shouldBeEqualTo listOf(1, 10)
    }

    @Test
    fun doesNotRemoveDuplicates() {
        list.addAll(listOf(1, 1, 1, 5, 6, 7))
        list.removeMany(listOf(1, 1, 7))
        list.toList() shouldBeEqualTo listOf(1, 5, 6)
    }

    @Test
    fun removeLargeBatch() {
        val add = (1..10000).toList()
        list.addAll(add)
        val remove = list.take(list.size / 2).shuffled().toList()
        list.removeMany(remove)

        list.toList() shouldBeEqualTo add.minus(remove).toList()
    }
}
