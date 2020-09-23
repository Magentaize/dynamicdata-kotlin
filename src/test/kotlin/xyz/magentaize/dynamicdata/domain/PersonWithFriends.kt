package xyz.magentaize.dynamicdata.domain

import xyz.magentaize.dynamicdata.kernel.NotifyPropertyChanged
import xyz.magentaize.dynamicdata.kernel.PropertyChangedEvent
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject

class PersonWithFriends(
    val name: String,
    age: Int,
    friends: Iterable<PersonWithFriends>
) : NotifyPropertyChanged {
    constructor(name: String, age: Int) : this(name, age, listOf())

    var age: Int = age
        set(value) {
            field = value
            propertyChanged.onNext(PropertyChangedEvent(this, "age"))
        }

    var friends: Iterable<PersonWithFriends> = friends
        set(value) {
            field = value
            propertyChanged.onNext(PropertyChangedEvent(this, "friends"))
        }

    override val propertyChanged: Subject<PropertyChangedEvent> =
        PublishSubject.create()
}
