package dynamicdata.domain

import dynamicdata.kernel.INotifyPropertyChanged
import dynamicdata.kernel.PropertyChangedEvent
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import java.util.*

internal class Item(name: String) : INotifyPropertyChanged {
    val id: UUID = UUID.randomUUID()
    var name: String = name
        set(value) {
            field = value
            propertyChanged.onNext(PropertyChangedEvent(this, "name"))
        }
    override val propertyChanged: Subject<PropertyChangedEvent> = PublishSubject.create()
}
