package xyz.magentaize.dynamicdata.binding

import xyz.magentaize.dynamicdata.kernel.NotifyPropertyChanged
import io.reactivex.rxjava3.core.Observable
import xyz.magentaize.dynamicdata.kernel.Stub
import kotlin.reflect.KProperty1

fun <T : NotifyPropertyChanged> T.whenPropertyChanged(vararg propertiesToMonitor: String): Observable<T> =
    this.propertyChanged
        .filter { propertiesToMonitor.isEmpty() || propertiesToMonitor.contains(it.propertyName) }
        .map { this }

fun <T : NotifyPropertyChanged, R> T.whenPropertyChanged(
    accessor: KProperty1<T, R>,
    notifyOnInitialValue: Boolean = true,
    fallbackValue: () -> R = Stub.emptyFallback()
): Observable<PropertyValue<T, R>> {
    val cache = ObservablePropertyFactoryCache.instance().getFactory(accessor)
    return cache.create(this, notifyOnInitialValue)
        .filter { !it.unobtainableValue || (it.unobtainableValue && fallbackValue != Stub.emptyFallback<R>()) }
}
