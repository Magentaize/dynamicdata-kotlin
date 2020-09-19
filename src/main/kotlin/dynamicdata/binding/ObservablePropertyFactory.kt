package dynamicdata.binding

import dynamicdata.kernel.NotifyPropertyChanged
import io.reactivex.rxjava3.core.Observable
import kotlin.reflect.KProperty1

internal class ObservablePropertyFactory<T : NotifyPropertyChanged, R>(accessor: KProperty1<T, R>) {
    private val _factory: (T, Boolean) -> Observable<PropertyValue<T, R>> = { t, notifyInitial ->
        fun factory(): PropertyValue<T, R> =
            PropertyValue(t, accessor(t))

        val propertyChanged = t.propertyChanged
            .filter { it.propertyName == accessor.name }
            .map { factory() }

        if (!notifyInitial) {
            propertyChanged
        } else {
            val initial = Observable.defer { Observable.just(factory()) }
            initial.concatWith(propertyChanged)
        }
    }

    fun create(source: T, notifyInitial: Boolean): Observable<PropertyValue<T, R>> =
        _factory(source, notifyInitial)
}
