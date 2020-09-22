package dynamicdata.list

import dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.disposables.Disposable
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class SubscribeManyFixture {
    val source = SourceList<SubscribeableObject>()
    val result = source.connect()
        .subscribeMany {
            it.subscribe()
            Disposable.fromAction(it::unsubscribe)
        }
        .asAggregator()

    @Test
    fun addedItemWillbeSubscribed() {
        source.add(SubscribeableObject(1))

        result.messages.size shouldBeEqualTo 1
        result.data.size shouldBeEqualTo 1
        result.data.items.first().isSubscribed shouldBeEqualTo true
    }

    @Test
    fun removeIsUnsubscribed() {
        source.add(SubscribeableObject(1))
        source.removeAt(0)

        result.messages.size shouldBeEqualTo 2
        result.data.size shouldBeEqualTo 0
        result.messages[1].first().item.current.isSubscribed shouldBeEqualTo false
    }

    @Test
    fun everythingIsUnsubscribedWhenStreamIsDisposed() {
        source.addRange((1..10).map { SubscribeableObject(it) })
        source.clear()

        result.messages.size shouldBeEqualTo 2
        result.messages[0].flatMap { it.range }.all { !it.isSubscribed } shouldBeEqualTo true
    }

    class SubscribeableObject(private val id: Int) {
        var isSubscribed: Boolean = false
            private set

        fun subscribe() {
            isSubscribed = true
        }

        fun unsubscribe() {
            isSubscribed = false
        }
    }
}
