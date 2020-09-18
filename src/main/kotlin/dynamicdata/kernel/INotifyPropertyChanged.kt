package dynamicdata.kernel

import io.reactivex.rxjava3.core.Observable

interface INotifyPropertyChanged {
    val propertyChanged: Observable<PropertyChangedEvent>
}
