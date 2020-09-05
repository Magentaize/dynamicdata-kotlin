package dynamicdata.list

import dynamicdata.list.test.asAggregator
import org.amshove.kluent.shouldBeEqualTo
import java.math.BigInteger
import kotlin.test.Test

internal class CastFixture {
    private val source = SourceList<Int>()
    private val results = source.cast { it.toBigInteger() }
        .asAggregator<BigInteger>()

    @Test
    fun canCast() {
        source.addRange(1..10)
        results.data.size shouldBeEqualTo 10

        source.clear()
        results.data.size shouldBeEqualTo 0
    }
}