package dynamicdata.domain

import dynamicdata.kernel.INotifyPropertyChanged
import dynamicdata.kernel.PropertyChangedEvent
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject

internal class Person(
    val name: String,
    age: Int,
    val gender: String = "F",
    val parentName: String = ""
) : INotifyPropertyChanged {
    var age = age
        set(value) {
            field = value
            propertyChanged.onNext(PropertyChangedEvent(this, "age"))
        }

    constructor(firstName: String, lastName: String, age: Int)
            : this("$firstName $lastName", age)

    override val propertyChanged: Subject<PropertyChangedEvent> = PublishSubject.create()

    override fun toString(): String {
        return "$name. $age"
    }
}
