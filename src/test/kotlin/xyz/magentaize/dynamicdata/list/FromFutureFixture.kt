package xyz.magentaize.dynamicdata.list

import xyz.magentaize.dynamicdata.domain.Person
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.TestScheduler
import org.amshove.kluent.`should not be`
import org.amshove.kluent.shouldBeEqualTo
import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class FromFutureFixture {
    @Test
    fun canLoadFromTask() {
        fun loader(): CompletableFuture<Iterable<Person>> {
            val items = (1..100).map { Person("P$it", it) }
                .toList()

            return CompletableFuture<Iterable<Person>>().apply { complete(items) }
        }

        val data = Observable.fromFuture(loader())
            .toObservableChangeSet()
            .asObservableList()

        data.size shouldBeEqualTo 100
    }

    @Test
    fun handlesErrorsInObservable() {
        fun loader(): CompletableFuture<Iterable<Person>> =
            CompletableFuture.supplyAsync {
                throw Exception("Broken")
            }

        var error: Throwable? = null

        Observable.fromFuture(loader())
            .toObservableChangeSet()
            .subscribe({}, { error = it })

        error `should not be` null
    }

    @Test
    fun handlesErrorsObservableList() {
        fun loader(): CompletableFuture<Iterable<Person>> =
            CompletableFuture.supplyAsync {
                throw Exception("Broken")
            }

        var error: Throwable? = null

        Observable.fromFuture(loader())
            .toObservableChangeSet()
            .asObservableList()
            .connect()
            .subscribe({}, { error = it })

        error `should not be` null
    }
}
