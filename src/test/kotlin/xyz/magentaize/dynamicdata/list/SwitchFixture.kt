package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.subjects.BehaviorSubject
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

class SwitchFixture {
    val source = SourceList<Int>()
    val switchable = BehaviorSubject.createDefault<EditableObservableList<Int>>(source)
    val result = switchable.switch().asAggregator()

    @Test
    fun clearsForNewSource() {
        val initial = (1..100).toList()
        source.addRange(initial)
        result.data.size shouldBeEqualTo 100
        source.items.toList() shouldBeEqualTo initial

        val new = SourceList<Int>()
        switchable.onNext(new)
        result.data.size shouldBeEqualTo 0

        new.addRange(initial)
        result.data.size shouldBeEqualTo 100

        val next = (100..199).toList()
        new.addRange(next)
        result.data.size shouldBeEqualTo 200
    }
}

