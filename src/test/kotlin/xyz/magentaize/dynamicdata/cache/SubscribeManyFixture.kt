package xyz.magentaize.dynamicdata.cache

import io.reactivex.rxjava3.disposables.Disposable
import org.amshove.kluent.shouldBeEqualTo
import xyz.magentaize.dynamicdata.cache.test.ChangeSetAggregator
import kotlin.test.Test

class SubscribeManyFixture {
    private val source = SourceCache<Int, SubscribableObject> { it.id }
    private val result = ChangeSetAggregator(
        source.connect()
            .subscribeMany { _, v ->
                v.isSubscribed = true
                return@subscribeMany Disposable.fromAction {
                    v.isSubscribed = false
                }
            }
    )

    @Test
    fun addedItemWillBeSubscribed() {
        source.addOrUpdate(SubscribableObject(1))

        result.messages.size shouldBeEqualTo 1
        result.data.size shouldBeEqualTo 1
        result.data.items.first().isSubscribed shouldBeEqualTo true
    }

    @Test
    fun everythingIsUnsubscribedWhenStreamIsDisposed() {
        source.addOrUpdate((1..10).map { SubscribableObject(it) })
        source.clear()

        result.messages.size shouldBeEqualTo 2
        result.messages[1].all { !it.current.isSubscribed } shouldBeEqualTo true
    }

    @Test
    fun removeIsUnsubscribed() {
        source.addOrUpdate(SubscribableObject(1))
        source.remove(1)

        result.messages.size shouldBeEqualTo 2
        result.data.size shouldBeEqualTo 0
        result.messages[1].first().current.isSubscribed shouldBeEqualTo false
    }

    @Test
    fun updateUnsubscribesPrevious() {
        source.addOrUpdate(SubscribableObject(1))
        source.addOrUpdate(SubscribableObject(1))

        result.messages.size shouldBeEqualTo 2
        result.data.size shouldBeEqualTo 1
        result.messages[1].first().current.isSubscribed shouldBeEqualTo true
        result.messages[1].first().previous.value.isSubscribed shouldBeEqualTo false
    }

    private class SubscribableObject(val id: Int) {
        var isSubscribed = false
    }
}
