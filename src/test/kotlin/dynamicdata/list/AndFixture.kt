package dynamicdata.list

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class AndFixture{ //: AndFixtureBase{
    @Test
    fun t(){
        ItemChange.empty<String>() shouldBe ItemChange.empty()
        ItemChange.empty<Int>() shouldBeEqualTo ItemChange.empty<String>()
    }
}
//
//internal abstract class AndFixtureBase : Disposable
//{
//    protected val ISourceList<Int> source1
//}
