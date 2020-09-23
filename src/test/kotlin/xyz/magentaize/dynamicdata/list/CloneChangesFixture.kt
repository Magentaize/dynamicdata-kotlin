package xyz.magentaize.dynamicdata.list

import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

class CloneChangesFixture {
    val source = ChangeAwareList<Int>()
    val clone = mutableListOf<Int>()

    fun assert() {
        clone.clone(source.captureChanges())
        clone.toList() shouldBeEqualTo source.toList()
    }

    @Test
    fun add() {
        source.add(1)
        assert()
    }

    @Test
    fun addSecond() {
        source.add(1)
        source.add(2)
        assert()
    }

    @Test
    fun addManyInSuccession() {
        (1..10).forEach(source::add)
        assert()
    }

    @Test
    fun addRange() {
        source.addAll(1..10)
        assert()
    }

    @Test
    fun addSecondRange() {
        source.addAll(1..10)
        source.addAll(11..20)
        assert()
    }

    @Test
    fun insertRangeInCentre() {
        source.addAll(1..10)
        source.addAll(5, 11..20)
        assert()
    }

    @Test
    fun remove() {
        source.add(1)
        source.remove(1)
        assert()
    }

    @Test
    fun removeRange() {
        source.addAll(1..10)
        source.removeAll(5, 3)
        assert()
    }

    @Test
    fun removeSuccession() {
        source.addAll(1..10)
        source.clearChanges()
        source.toList().forEach(source::remove)
        assert()
    }

    @Test
    fun removeSuccessionReversed() {
        source.addAll(1..10)
        source.clearChanges()
        source.sortedByDescending { it }.toList().forEach(source::remove)
        assert()
    }

    @Test
    fun removeMany() {
        source.addAll(1..10)
        source.removeMany(1..10)
        assert()
    }

    @Test
    fun removeInnerRange() {
        source.addAll(1..10)
        source.removeAll(5, 3)
        assert()
    }

    @Test
    fun removeManyPartial() {
        source.addAll(1..10)
        source.removeMany(3..7)
        assert()
    }

    @Test
    fun movedItemInListLowerToHigherIsMoved() {
        source.addAll(1..10)
        source.move(1, 2)
        assert()
    }

    @Test
    fun movedItemInListHigherToLowerIsMoved() {
        source.addAll(1..10)
        source.move(2, 1)
        assert()
    }
}
