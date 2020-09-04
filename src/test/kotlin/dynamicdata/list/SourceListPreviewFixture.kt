package dynamicdata.list

import dynamicdata.list.test.asAggregator
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldThrow
import kotlin.test.Test

internal class SourceListPreviewFixture {
    private val source = SourceList<Int>()

    @Test
    fun noChangesAllowedDuringPreview() {
        // On preview, try adding an arbitrary item
        val d = source.preview().subscribe {
            invoking { source.add(1) }
                .shouldThrow(IllegalStateException::class)
        }
        source.add(1)
        d.dispose()
    }

    @Test
    fun recursiveEditsWork() {
        source.edit { L1 ->
            source.edit { L2 -> L2.add(1) }
            source.items.toList() shouldBeEqualTo listOf(1)
            L1.toList() shouldBeEqualTo listOf(1)
        }
        source.items.toList() shouldBeEqualTo listOf(1)
    }

    @Test
    fun recursiveEditsHavePostponedEvents() {
        val preview = source.preview().asAggregator()
        val connect = source.connect().asAggregator()

        source.edit { L1 ->
            source.edit { L2 -> L2.add(1) }
            preview.messages.size shouldBeEqualTo 0
            connect.messages.size shouldBeEqualTo 0
        }
        preview.messages.size shouldBeEqualTo 1
        connect.messages.size shouldBeEqualTo 1
        source.items.toList() shouldBeEqualTo listOf(1)
    }

    @Test
    fun previewEventsAreCorrect() {
        val preview = source.preview().asAggregator()
        val connect = source.connect().asAggregator()

        source.edit { L1 ->
            L1.add(1)
            source.edit { L2 -> L2.add(2) }
            L1.remove(2)
            L1.addAll(listOf(3, 4, 5))
            L1.move(1, 0)
        }

        preview.messages.toList() shouldBeEqualTo connect.messages.toList()
        source.items.toList() shouldBeEqualTo listOf(3, 1, 4, 5)
    }

    @Test
    fun changesAreNotYetAppliedDuringPreview() {
        source.clear()
        // On preview, make sure the list is empty
        val d = source.preview().subscribe {
            source.size shouldBeEqualTo 0
            source.items.count() shouldBeEqualTo 0
        }

        source.add(1)
        d.dispose()
    }

    @Test
    fun connectPreviewPredicateIsApplied() {
        source.clear()
        // Collect preview messages about even numbers only
        val aggregator = source.preview { it % 2 == 0 }.asAggregator()
        source.add(1)
        source.add(2)

        aggregator.messages.size shouldBeEqualTo 1
        aggregator.messages[0].size shouldBeEqualTo 1
        aggregator.messages[0].first().item.current shouldBeEqualTo 2
        aggregator.messages[0].first().reason shouldBeEqualTo ListChangeReason.Add

        aggregator.dispose()
    }

    @Test
    fun formNewListFromChanges() {
        source.clear()
        source.addRange(1..100)
        val aggregator = source.preview { it % 2 == 0 }.asAggregator()
        source.removeAt(10)
        source.removeRange(10, 5)
        source.add(1)
        source.add(2)

        aggregator.messages.size shouldBeEqualTo 1
        aggregator.messages[0].size shouldBeEqualTo 1
        aggregator.messages[0].first().item.current shouldBeEqualTo 2
        aggregator.messages[0].first().reason shouldBeEqualTo ListChangeReason.Add

        aggregator.dispose()
    }
}
