package dynamicdata.list

import dynamicdata.domain.PersonWithRelations
import dynamicdata.list.test.asAggregator
import dynamicdata.utilities.recursiveMap
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

class TransformManyFixture {
    val source = SourceList<PersonWithRelations>()
    val result = source.connect()
        .transformMany { it.relations.recursiveMap { p -> p.relations } }
        .asAggregator()

    @Test
    fun add() {
        val f1 = PersonWithRelations("Friend1", 10)
        val c1 = PersonWithRelations("Child1", 10, listOf(f1))
        val c2 = PersonWithRelations("Child2", 8)
        val c3 = PersonWithRelations("Child3", 8)
        val m = PersonWithRelations("Mother", 35, listOf(c1, c2, c3))
        source.add(m)

        result.data.size shouldBeEqualTo 4
        result.data.items.toSet() shouldBeEqualTo setOf(c1, c2, c3, f1)
    }

    @Test
    fun removeParent() {
        val f1 = PersonWithRelations("Friend1", 10)
        val c1 = PersonWithRelations("Child1", 10, listOf(f1))
        val c2 = PersonWithRelations("Child2", 8)
        val c3 = PersonWithRelations("Child3", 8)
        val m = PersonWithRelations("Mother", 35, listOf(c1, c2, c3))
        source.add(m)
        source.remove(m)

        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun replace() {
        val f1 = PersonWithRelations("Friend1", 10)
        val c1 = PersonWithRelations("Child1", 10, listOf(f1))
        val c2 = PersonWithRelations("Child2", 8)
        val c3 = PersonWithRelations("Child3", 8)
        val m = PersonWithRelations("Mother", 35, listOf(c1, c2, c3))
        source.add(m)
        val c4 = PersonWithRelations("Child4", 2)
        val updatedM = PersonWithRelations("Mother", 35, listOf(c1, c2, c4))
        source.replace(m, updatedM)

        result.data.size shouldBeEqualTo 4
        result.data.items.toSet() shouldBeEqualTo setOf(c1, c2, c4, f1)
    }

    @Test
    fun addRange() {
        val f1 = PersonWithRelations("Friend1", 10)
        val c1 = PersonWithRelations("Child1", 10, listOf(f1))
        val c2 = PersonWithRelations("Child2", 8)
        val c3 = PersonWithRelations("Child3", 8)
        val m = PersonWithRelations("Mother", 35, listOf(c1, c2, c3))
        source.add(m)

        val c4 = PersonWithRelations("Child4", 1)
        val c5 = PersonWithRelations("Child5", 2)
        val a1 = PersonWithRelations("Another1", 2, listOf(c4, c5))
        val c6 = PersonWithRelations("Child6", 1)
        val c7 = PersonWithRelations("Child7", 2)
        val a2 = PersonWithRelations("Another2", 2, listOf(c6, c7))
        source.addRange(listOf(a1, a2))

        result.data.size shouldBeEqualTo 8
        result.data.items.toSet() shouldBeEqualTo setOf(c1, c2, c3, f1, c4, c5, c6, c7)
    }

    @Test
    fun removeRange() {
        val f1 = PersonWithRelations("Friend1", 10)
        val c1 = PersonWithRelations("Child1", 10, listOf(f1))
        val c2 = PersonWithRelations("Child2", 8)
        val c3 = PersonWithRelations("Child3", 8)
        val m = PersonWithRelations("Mother", 35, listOf(c1, c2, c3))
        val c4 = PersonWithRelations("Child4", 1)
        val c5 = PersonWithRelations("Child5", 2)
        val a1 = PersonWithRelations("Another1", 2, listOf(c4, c5))
        val c6 = PersonWithRelations("Child6", 1)
        val c7 = PersonWithRelations("Child7", 2)
        val a2 = PersonWithRelations("Another2", 2, listOf(c6, c7))
        source.addRange(listOf(m, a1, a2))
        source.removeRange(0, 2)

        result.data.size shouldBeEqualTo 2
        result.data.items.toSet() shouldBeEqualTo setOf(c6, c7)
    }

    @Test
    fun clear() {
        val f1 = PersonWithRelations("Friend1", 10)
        val c1 = PersonWithRelations("Child1", 10, listOf(f1))
        val c2 = PersonWithRelations("Child2", 8)
        val c3 = PersonWithRelations("Child3", 8)
        val m = PersonWithRelations("Mother", 35, listOf(c1, c2, c3))
        val c4 = PersonWithRelations("Child4", 1)
        val c5 = PersonWithRelations("Child5", 2)
        val a1 = PersonWithRelations("Another1", 2, listOf(c4, c5))
        val c6 = PersonWithRelations("Child6", 1)
        val c7 = PersonWithRelations("Child7", 2)
        val a2 = PersonWithRelations("Another2", 2, listOf(c6, c7))
        source.addRange(listOf(m, a1, a2))
        source.clear()

        result.data.size shouldBeEqualTo 0
    }

    @Test
    fun move() {
        val c4 = PersonWithRelations("Child4", 1)
        val c5 = PersonWithRelations("Child5", 2)
        val a1 = PersonWithRelations("Another1", 2, listOf(c4, c5))
        val c6 = PersonWithRelations("Child6", 1)
        val c7 = PersonWithRelations("Child7", 2)
        val a2 = PersonWithRelations("Another2", 2, listOf(c6, c7))
        source.addRange(listOf(a1, a2))
        result.messages.size shouldBeEqualTo 1

        source.move(1, 0)
        result.messages.size shouldBeEqualTo 1
    }

//    @Test
//    fun remove() {
//        val providers = SourceList<TourProvider>()
//        val allTours = providers.connect()
//            .transformMany { it.tours }
//            .asObservableList()
//
//        val tour1_1 = Tour("Tour 1.1")
//        val tour2_1 = Tour("Tour 2.1")
//        val tour2_2 = Tour("Tour 2.2")
//        val tour3_1 = Tour("Tour 3.1")
//        val tp1 = TourProvider("TP1", mutableListOf(tour1_1))
//        val tp2 = TourProvider("TP2", mutableListOf(tour2_1, tour2_2))
//        val tp3 = TourProvider("TP3", mutableListOf())
//        providers.addRange(listOf(tp1, tp2, tp3))
//        allTours.items.toList() shouldBeEqualTo listOf(tour1_1, tour2_1, tour2_2)
//
//        tp3.tours.add(tour3_1)
//        allTours.items.toList() shouldBeEqualTo listOf(tour1_1, tour2_1, tour2_2, tour3_1)
//
//        tp2.tours.remove(tour2_1)
//        allTours.items.toList() shouldBeEqualTo listOf(tour1_1, tour2_2, tour3_1)
//
//        tp2.tours.add(tour2_1)
//        allTours.items.toList() shouldBeEqualTo listOf(tour1_1, tour2_1, tour2_2, tour3_1)
//    }
//
//    class TourProvider(
//        val name: String,
//        val tours: MutableList<Tour>
//    )
//
//    class Tour(val name: String){
//        override fun toString(): String =
//            "name: $name"
//    }
}
