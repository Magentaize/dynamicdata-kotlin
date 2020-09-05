package dynamicdata.list

import dynamicdata.list.test.asAggregator
import io.reactivex.rxjava3.disposables.Disposable
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class DisposeManyFixture {
    private val source = SourceList<DisposableObject>()
    private val result = source.connect().disposeMany().asAggregator()

    @Test
    fun addWillNotCallDispose() {
        source.add(DisposableObject(1))

        result.messages.size shouldBeEqualTo 1
        result.data.size shouldBeEqualTo 1
        result.data.items.first().isDisposed shouldBeEqualTo false
    }

    @Test
    fun removeWillCallDispose() {
        source.add(DisposableObject(1))
        source.removeAt(0)

        result.messages.size shouldBeEqualTo 2
        result.data.size shouldBeEqualTo 0
        result.messages[1].first().item.current.isDisposed shouldBeEqualTo true
    }

    @Test
    fun updateWillCallDispose() {
        source.add(DisposableObject(1))
        source.replaceAt(0, DisposableObject(1))

        result.messages.size shouldBeEqualTo 2
        result.data.size shouldBeEqualTo 1
        result.messages[1].first().item.current.isDisposed shouldBeEqualTo false
        result.messages[1].first().item.previous.value.isDisposed shouldBeEqualTo true
    }

    @Test
    fun everythingIsDisposedWhenStreamIsDisposed() {
        val adds = (1..10).map { DisposableObject(it) }.toList()
        source.addRange(adds)
        source.clear()

        result.messages.size shouldBeEqualTo 2

        val itemsCleared = result.messages[1].first().range
        itemsCleared.all { it.isDisposed } shouldBeEqualTo true
    }

    private class DisposableObject(val id: Int) : Disposable {
        private var disposed = false

        override fun dispose() {
            disposed = true
        }

        override fun isDisposed(): Boolean {
            return disposed
        }
    }
}