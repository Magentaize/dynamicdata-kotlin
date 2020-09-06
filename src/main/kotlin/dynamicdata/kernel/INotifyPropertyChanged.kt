package dynamicdata.kernel

import io.reactivex.rxjava3.subjects.Subject

interface INotifyPropertyChanged {
    val propertyChanged: Subject<PropertyChangedEvent>
}
