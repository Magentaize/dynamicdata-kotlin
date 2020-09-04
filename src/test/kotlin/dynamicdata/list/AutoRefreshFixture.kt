//package dynamicdata.list
//
//import dynamicdata.domain.Person
//import kotlin.test.Test
//
//internal class AutoRefreshFixture {
//    @Test
//    fun autoRefresh(){
//        val items = List(100){Person("Person$it", 1)}
//
//        val list = SourceList<Person>()
//        val results = list.connect().autoRefresh{it.age}
//    }
//}
