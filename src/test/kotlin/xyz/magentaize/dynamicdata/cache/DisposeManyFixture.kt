package xyz.magentaize.dynamicdata.cache

import io.reactivex.rxjava3.disposables.Disposable
import org.amshove.kluent.shouldBe
import kotlin.test.Test
import xyz.magentaize.dynamicdata.cache.test.ChangeSetAggregator

class DisposeManyFixture {
    private val source = SourceCache<Int, DisposableObject> { it.id }
    private val result = ChangeSetAggregator(source.connect().disposeMany())

    @Test
    fun addWillNotCallDispose() {
        source.addOrUpdate(DisposableObject(1))

        result.messages.size shouldBe 1
        result.data.size shouldBe 1
        result.data.items.first().isDisposed shouldBe false
    }

    @Test
    fun everythingIsDisposedWhenStreamIsDisposed() {
        source.addOrUpdate((1..10).map { DisposableObject(it) })
        source.clear()

        result.messages.size shouldBe 2
        result.messages[1].all { it.current.isDisposed } shouldBe true
    }

    @Test
    fun removeWillCallDispose() {
        source.addOrUpdate(DisposableObject(1))
        source.remove(1)

        result.messages.size shouldBe 2
        result.data.size shouldBe 0
        result.messages[1].first().current.isDisposed shouldBe true
    }

    @Test
    fun updateWillCallDispose() {
        source.addOrUpdate(DisposableObject(1))
        source.addOrUpdate(DisposableObject(1))

        result.messages.size shouldBe 2
        result.data.size shouldBe 1
        result.messages[1].first().current.isDisposed shouldBe false
        result.messages[1].first().previous.value.isDisposed shouldBe true
    }

    class DisposableObject(val id: Int) : Disposable {
        private var _isDisposed = false

        override fun dispose() {
            _isDisposed = true
        }

        override fun isDisposed(): Boolean =
            _isDisposed
    }
}