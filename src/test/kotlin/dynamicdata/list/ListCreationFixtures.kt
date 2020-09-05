package dynamicdata.list

import io.reactivex.rxjava3.core.Observable
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import kotlin.test.Test

@Test
fun create(){

}

fun <T> subscribeAndAssert(changes: Observable<IChangeSet<T>>, expectsError: Boolean = false){
    var error: Throwable? = null
    var complete = false
    var change: IChangeSet<T>
    val myList = changes.doFinally { complete = true }.asObservableList()
    myList.connect().subscribe({change = it}, {error = it})

    if(!expectsError)
        error shouldBeEqualTo null
    else
        error shouldNotBeEqualTo null

    complete shouldBeEqualTo true
}