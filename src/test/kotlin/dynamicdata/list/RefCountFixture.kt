package dynamicdata.list

import dynamicdata.domain.Person
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class RefCountFixture {
    val source = SourceList<Person>()

    @Test
    fun chainIsInvokedOnceForMultipleSubscribers() {
        var created = 0
        var disposals = 0

        val longChain = source.connect()
            .transform { it }
            .doOnEach { created++ }
            .doFinally { disposals++ }
            .refCount()

        val s1 = longChain.subscribe()
        val s2 = longChain.subscribe()
        val s3 = longChain.subscribe()

        source.add(Person("Name", 10))
        s1.dispose()
        s2.dispose()
        s3.dispose()

        created shouldBeEqualTo 1
        disposals shouldBeEqualTo 1
    }

    @Test
    fun canResubscribe() {
        var created = 0
        var disposals = 0
        source.add(Person("Name", 10))

        val longChain = source.connect()
            .transform { it }
            .doOnEach { created++ }
            .doFinally { disposals++ }
            .refCount()

        var s = longChain.subscribe()
        s.dispose()

        s = longChain.subscribe()
        s.dispose()

        created shouldBeEqualTo 2
        disposals shouldBeEqualTo 2
    }
}
