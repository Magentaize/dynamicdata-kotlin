package dynamicdata.list

import dynamicdata.cache.AnonymousPageRequest
import dynamicdata.cache.PageRequest
import dynamicdata.domain.Animal
import dynamicdata.domain.AnimalFamily
import dynamicdata.domain.Person
import dynamicdata.domain.RandomPersonGenerator
import dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class PageFixture {
    val source = SourceList<Person>()
    val pageRequest: BehaviorSubject<PageRequest> = BehaviorSubject.createDefault(AnonymousPageRequest(1, 25))
    val result = source.connect().page(pageRequest).asAggregator()

    @Test
    fun virtualizeInitial() {
        val people = RandomPersonGenerator.take(100).toList()
        source.addRange(people)

        result.data.items.toList() shouldBeEqualTo people.take(25).toList()
    }

    @Test
    fun moveToNextPage() {
        val people = RandomPersonGenerator.take(100).toList()
        source.addRange(people)
        pageRequest.onNext(AnonymousPageRequest(2, 25))

        result.data.items.toList() shouldBeEqualTo people.drop(25).take(25).toList()
    }

    @Test
    fun insertAfterPageProducesNothing() {
        val people = RandomPersonGenerator.take(100).toList()
        source.addRange(people)
        source.addRange(RandomPersonGenerator.take(100), 50)

        result.data.items.toList() shouldBeEqualTo people.take(25).toList()
    }

    @Test
    fun insertInPageReflectsChange() {
        val people = RandomPersonGenerator.take(100).toList()
        source.addRange(people)
        val p = Person("A", 1)
        source.add(10, p)
        val msg = result.messages[1].elementAt(0)
        val removedP = people.elementAt(24)

        result.data.items.elementAt(10) shouldBe p
        msg.item.current shouldBe removedP
        msg.reason shouldBeEqualTo ListChangeReason.Remove
    }

    @Test
    fun removeBeforeShiftsPage() {
        val people = RandomPersonGenerator.take(100).toList()
        source.addRange(people)
        pageRequest.onNext(AnonymousPageRequest(2, 25))
        source.removeAt(0)
        result.data.items.toList() shouldBeEqualTo people.drop(26).take(25).toList()

        val re = result.messages[2].elementAt(0)
        val reP = people.elementAt(25)
        re.item.current shouldBeEqualTo reP
        re.reason shouldBeEqualTo ListChangeReason.Remove

        val add = result.messages[2].elementAt(1)
        val addP = people.elementAt(50)
        add.item.current shouldBe addP
        add.reason shouldBeEqualTo ListChangeReason.Add
    }

    @Test
    fun moveWithinSamePage() {
        val people = RandomPersonGenerator.take(100).toList()
        source.addRange(people)
        val personToMove = people[10]
        source.move(10, 0)

        result.data.items.elementAt(0) shouldBe personToMove
    }

    @Test
    fun simplePaging() {
        val items = listOf(
            Animal("Holly", "Cat", AnimalFamily.Mammal),
            Animal("Rover", "Dog", AnimalFamily.Mammal),
            Animal("Rex", "Dog", AnimalFamily.Mammal),
            Animal("Whiskers", "Cat", AnimalFamily.Mammal),
            Animal("Nemo", "Fish", AnimalFamily.Fish),
            Animal("Moby Dick", "Whale", AnimalFamily.Mammal),
            Animal("Fred", "Frog", AnimalFamily.Amphibian),
            Animal("Isaac", "Next", AnimalFamily.Amphibian),
            Animal("Sam", "Snake", AnimalFamily.Reptile),
            Animal("Sharon", "Red Backed Shrike", AnimalFamily.Bird),
        )
        val source = SourceList<Animal>()
        pageRequest.onNext(AnonymousPageRequest(0, 0))
        val sut = SimplePaging(source, pageRequest)

        source.addRange(items)
        sut.paged.size shouldBeEqualTo 0

        pageRequest.onNext(AnonymousPageRequest(1, 2))
        sut.paged.size shouldBeEqualTo 2

        pageRequest.onNext(AnonymousPageRequest(1, 4))
        sut.paged.size shouldBeEqualTo 4

        pageRequest.onNext(AnonymousPageRequest(2, 3))
        sut.paged.size shouldBeEqualTo 3
    }

    class SimplePaging(
        val source: IObservableList<Animal>,
        pager: Observable<PageRequest>
    ) {
        val paged: IObservableList<Animal> =
            source.connect()
                .page(pager)
                .asObservableList()
    }
}
