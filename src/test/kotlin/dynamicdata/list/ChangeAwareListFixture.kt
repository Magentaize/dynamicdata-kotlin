package dynamicdata.list

import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldThrow
import java.lang.IllegalArgumentException
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
    fun addSecond() {
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
    fun addManyInSuccession() {
        IntRange(1, 10).forEach { list.add(it) }
        val changes = list.captureChanges()

        changes.count() shouldBeEqualTo 1
        changes.adds shouldBeEqualTo 10
        changes.first().range.toList() shouldBeEqualTo (1..10).toList()
        list.toList() shouldBeEqualTo (1..10).toList()
    }

    @Test
    fun addRange() {
        list.addAll(1..10)
        val changes = list.captureChanges()

        changes.count() shouldBeEqualTo 1
        changes.adds shouldBeEqualTo 10
        changes.first().range.toList() shouldBeEqualTo (1..10).toList()
        list.toList() shouldBeEqualTo (1..10).toList()
    }

    @Test
    fun addSecondRange() {
        list.addAll(1..10)
        list.addAll(11..20)
        val changes = list.captureChanges()

        changes.count() shouldBeEqualTo 2
        changes.adds shouldBeEqualTo 20
        changes.first().range.toList() shouldBeEqualTo (1..10).toList()
        changes.drop(1).first().range.toList() shouldBeEqualTo (11..20).toList()
        list.toList() shouldBeEqualTo (1..20).toList()
    }

    @Test
    fun insertRangeInCentre() {
        list.addAll(1..10)
        list.addAll(5, 11..20)
        val changes = list.captureChanges()

        changes.size shouldBeEqualTo 2
        changes.adds shouldBeEqualTo 20
        changes.first().range.toList() shouldBeEqualTo (1..10).toList()
        changes.drop(1).first().range.toList() shouldBeEqualTo (11..20).toList()

        val shouldBe = (1..5) union (11..20) union (6..10)
        list.toList() shouldBeEqualTo shouldBe.toList()
    }

    @Test
    fun remove() {
        list.add(1)
        list.clearChanges()
        list.remove(1)
        val changes = list.captureChanges()

        changes.size shouldBeEqualTo 1
        changes.removes shouldBeEqualTo 1
        changes.first().item.current shouldBeEqualTo 1
        list.size shouldBeEqualTo 0
    }

    @Test
    fun removeRange() {
        list.addAll(1..10)
        list.clearChanges()
        list.removeAll(5, 3)
        val changes = list.captureChanges()

        changes.size shouldBeEqualTo 1
        changes.removes shouldBeEqualTo 3
        changes.first().range.toList() shouldBeEqualTo (6..8).toList()

        val shouldBe = (1..5) union (9..10)
        list.toList() shouldBeEqualTo shouldBe.toList()
    }

    @Test
    fun removeSucession() {
        list.addAll(1..10)
        list.clearChanges()
        list.toList().forEach { list.remove(it) }
        val changes = list.captureChanges()

        changes.size shouldBeEqualTo 1
        changes.removes shouldBeEqualTo 10
        changes.first().range.toList() shouldBeEqualTo (1..10).toList()
        list.size shouldBeEqualTo 0
    }

    @Test
    fun removeSucessionReversed() {
        list.addAll(1..10)
        list.clearChanges()
        list.sortedByDescending { it }.toList().forEach { list.remove(it) }
        val changes = list.captureChanges()

        changes.size shouldBeEqualTo 1
        changes.removes shouldBeEqualTo 10
        changes.first().range.toList() shouldBeEqualTo (1..10).toList()
        list.size shouldBeEqualTo 0
    }

    @Test
    fun removeMany() {
        list.addAll(1..10)
        list.clearChanges()
        list.removeMany(1..10)
        val changes = list.captureChanges()

        changes.size shouldBeEqualTo 1
        changes.removes shouldBeEqualTo 10
        changes.first().range.toList() shouldBeEqualTo (1..10).toList()
        list.size shouldBeEqualTo 0
    }

    @Test
    fun refreshAt() {
        list.addAll(0..8)
        list.clearChanges()
        list.refreshAt(1)
        val changes = list.captureChanges()

        changes.size shouldBeEqualTo 1
        changes.refreshes shouldBeEqualTo 1
        changes.first().reason shouldBeEqualTo ListChangeReason.Refresh
        changes.first().item.current shouldBeEqualTo 1
        invoking { list.refreshAt(-1) }
            .shouldThrow(IllegalArgumentException::class)

        invoking { list.refreshAt(1000) }
            .shouldThrow(IllegalArgumentException::class)

    }

    @Test
    fun refresh() {
        list.addAll(0..8)
        list.clearChanges()
        list.refresh(1)
        val changes = list.captureChanges()

        changes.size shouldBeEqualTo 1
        changes.refreshes shouldBeEqualTo 1
        changes.first().reason shouldBeEqualTo ListChangeReason.Refresh
        changes.first().item.current shouldBeEqualTo 1
        list.refresh(5) shouldBeEqualTo true
        list.refresh(-1) shouldBeEqualTo false
        list.refresh(1000) shouldBeEqualTo false
    }

    @Test
    fun throwWhenRemovingItemOutsideOfBoundaries() {
        invoking { list.removeAt(0) }
            .shouldThrow(IndexOutOfBoundsException::class)
    }

    @Test
    fun throwWhenRemovingRangeThatBeginsOutsideOfBoundaries() {
        invoking { list.removeAll(0, 1) }
            .shouldThrow(IndexOutOfBoundsException::class)
    }

    @Test
    fun throwWhenRemovingRangeThatFinishesOutsideOfBoundaries() {
        list.add(0)
        invoking { list.removeAll(0, 2) }
            .shouldThrow(IndexOutOfBoundsException::class)
    }
}
