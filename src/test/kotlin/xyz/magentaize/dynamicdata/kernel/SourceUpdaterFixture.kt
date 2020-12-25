package xyz.magentaize.dynamicdata.kernel

import xyz.magentaize.dynamicdata.cache.ChangeAwareCache
import xyz.magentaize.dynamicdata.cache.ChangeReason
import xyz.magentaize.dynamicdata.cache.internal.CacheUpdater
import xyz.magentaize.dynamicdata.domain.Person
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class SourceUpdaterFixture {
    private val cache = ChangeAwareCache<String, Person>()
    private val updater = CacheUpdater(cache) { it.name }
    private val key = "Adult1"

    @Test
    fun add() {
        val person = Person("Adult1", 50)
        updater.addOrUpdate(person, "Adult1")
        val updates = cache.captureChanges()

        cache.lookup("Adult1").value shouldBeEqualTo person
        cache.size shouldBeEqualTo 1
        1 shouldBeEqualTo updates.count()
        // TODO
        //updates.first() shouldBe Change(ChangeReason.Add, person.name, person)
    }

    @Test
    fun attemptedRemovalOfANonExistentKeyWillBeIgnored() {
        updater.remove(key)
        val updates = cache.captureChanges()

        cache.size shouldBeEqualTo 0
        updates.size shouldBeEqualTo 0
    }

    @Test
    fun batchOfUniqueUpdates() {
        val people = (1..100).map { Person("Name$it", it) }.toList()
        updater.addOrUpdate(people)
        val updates = cache.captureChanges()

        cache.items.toList() shouldBeEqualTo people
        cache.size shouldBeEqualTo 100
        updates.adds shouldBeEqualTo 100
        updates.size shouldBeEqualTo 100
    }

    @Test
    fun batchRemoves() {
        val people = (1..100).map { Person("Name$it", it) }
        updater.addOrUpdate(people)
        updater.removeItem(people)
        val updates = cache.captureChanges()

        cache.size shouldBeEqualTo 0
        100 shouldBeEqualTo updates.count { it.reason == ChangeReason.Add }
        100 shouldBeEqualTo updates.count { it.reason == ChangeReason.Remove }
        200 shouldBeEqualTo updates.size
    }

    @Test
    fun batchSuccessiveUpdates() {
        val people = (1..100).map { Person("Name1", it) }
        updater.addOrUpdate(people)
        val updates = cache.captureChanges()

        cache.lookup("Name1").value.age shouldBeEqualTo 100
        cache.size shouldBeEqualTo 1
        99 shouldBeEqualTo updates.count { it.reason == ChangeReason.Update }
        1 shouldBeEqualTo updates.count { it.reason == ChangeReason.Add }
        100 shouldBeEqualTo updates.size
    }

    @Test
    fun canRemove() {
        val newPerson = Person(key, 50)
        val updated = Person(key, 51)
        updater.addOrUpdate(newPerson)
        updater.removeItem(updated)
        val updates = cache.captureChanges()

        cache.size shouldBeEqualTo 0
        1 shouldBeEqualTo updates.count { it.reason == ChangeReason.Add }
        1 shouldBeEqualTo updates.count { it.reason == ChangeReason.Remove }
        2 shouldBeEqualTo updates.size
    }

    @Test
    fun canUpdate() {
        val newPerson = Person(key, 50)
        val updated = Person(key, 51)
        updater.addOrUpdate(newPerson)
        updater.addOrUpdate(updated)
        val updates = cache.captureChanges()

        cache.lookup(key).value shouldBe updated
        cache.size shouldBeEqualTo 1
        1 shouldBeEqualTo updates.count { it.reason == ChangeReason.Add }
        1 shouldBeEqualTo updates.count { it.reason == ChangeReason.Update }
        2 shouldBeEqualTo updates.size
    }

    @Test
    fun clear() {
        val people = (1..100).map { Person("Name$it", it) }
        updater.addOrUpdate(people)
        updater.clear()
        val updates = cache.captureChanges()

        cache.size shouldBeEqualTo 0
        100 shouldBeEqualTo updates.count { it.reason == ChangeReason.Add }
        100 shouldBeEqualTo updates.count { it.reason == ChangeReason.Remove }
        200 shouldBeEqualTo updates.size
    }
}
