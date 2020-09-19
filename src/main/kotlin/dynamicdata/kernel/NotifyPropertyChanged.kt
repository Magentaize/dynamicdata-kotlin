package dynamicdata.kernel

import io.reactivex.rxjava3.core.Observable

interface NotifyPropertyChanged {
    val propertyChanged: Observable<PropertyChangedEvent>
}
