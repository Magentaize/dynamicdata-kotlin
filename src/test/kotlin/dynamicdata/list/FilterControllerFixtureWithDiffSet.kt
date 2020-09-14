package dynamicdata.list

import dynamicdata.domain.Person
import dynamicdata.list.test.ChangeSetAggregator
import dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class FilterControllerFixtureWithDiffSet : FilterBaseFixture() {
    override val filter: Subject<(Person) -> Boolean> =
        BehaviorSubject.createDefault { it.age > 20 }

    override val result: ChangeSetAggregator<Person> =
        source.connect().filterItem(filter).asAggregator()

    @Test
    fun changeFilter() {
        val people = (1..100).map { Person("P$it", it) }.toList()
        source.addRange(people)
        result.data.size shouldBeEqualTo 80

        filter.onNext { it.age <= 50 }
        result.data.size shouldBeEqualTo 50
        result.messages.size shouldBeEqualTo 2
        result.data.items.all { it.age <= 50 } shouldBeEqualTo true
    }

    @Test
    fun reevaluateFilter() {
        val people = (1..100).map { Person("P$it", it) }.toList()
        source.addRange(people)
        result.data.size shouldBeEqualTo 80

        people.forEach { it.age += 10 }
        filter.onNext { it.age > 20 }
        result.data.size shouldBeEqualTo 90
        result.messages.size shouldBeEqualTo 2
        result.messages[1].adds shouldBeEqualTo 10

        people.forEach { it.age -= 10 }
        filter.onNext { it.age > 20 }
        result.data.size shouldBeEqualTo 80
        result.messages.size shouldBeEqualTo 3
    }
}
