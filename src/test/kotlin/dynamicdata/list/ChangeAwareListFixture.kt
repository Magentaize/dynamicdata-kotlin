package dynamicdata.list

import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class ChangeAwareListFixture {
    private val list = ChangeAwareList<Int>()

    @Test
    fun add() {
        list.add(1)
        val changes = list.captureChanges()

        changes.count() shouldBeEqualTo 1
        changes.adds shouldBeEqualTo 1
        changes.first().item.current shouldBeEqualTo 1
        list.toList() shouldBeEqualTo listOf(1)
    }

    @Test
    fun addSecond(){
        list.add(1)
        list.clearChanges()
        list.add(2)
        val changes = list.captureChanges()

        changes.count() shouldBeEqualTo 1
        changes.adds shouldBeEqualTo 1
        changes.first().item.current shouldBeEqualTo 2
        list.toList() shouldBeEqualTo listOf(1, 2)
    }

    @Test
    fun addManyInSuccession(){
        IntRange(1, 10).forEach{list.add(it)}
        val changes = list.captureChanges()

        changes.count() shouldBeEqualTo 1
        changes.adds shouldBeEqualTo 10
        changes.first().range.toList() shouldBeEqualTo (1..10).toList()
        list.toList() shouldBeEqualTo (1..10).toList()
    }

    @Test
    fun addRange(){
        list.addAll(1..10)
        val changes = list.captureChanges()

        changes.count() shouldBeEqualTo 1
        changes.adds shouldBeEqualTo 10
        changes.first().range.toList() shouldBeEqualTo (1..10).toList()
        list.toList() shouldBeEqualTo (1..10).toList()
    }

    @Test
    fun addSecondRange(){
        list.addAll(1..10)
        list.addAll(11..20)
        val changes = list.captureChanges()

        changes.count() shouldBeEqualTo 2
        changes.adds shouldBeEqualTo 20
        changes.first().range.toList() shouldBeEqualTo (1..10).toList()
        changes.drop(1).first().range.toList() shouldBeEqualTo (11..20).toList()
        list.toList() shouldBeEqualTo (1..20).toList()
    }
}
