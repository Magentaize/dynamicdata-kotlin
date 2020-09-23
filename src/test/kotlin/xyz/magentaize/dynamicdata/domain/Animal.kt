package xyz.magentaize.dynamicdata.domain

import xyz.magentaize.dynamicdata.kernel.NotifyPropertyChanged
import xyz.magentaize.dynamicdata.kernel.PropertyChangedEvent
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject

class Animal(
    val name: String,
    val type: String,
    val family: AnimalFamily
) : NotifyPropertyChanged {
    override val propertyChanged: Subject<PropertyChangedEvent> =
        PublishSubject.create()

    var includeInResults: Boolean = false
        set(value) {
            field = value
            propertyChanged.onNext(PropertyChangedEvent(this, "includeInResults"))
        }
}

enum class AnimalFamily {
    Mammal,
    Reptile,
    Fish,
    Amphibian,
    Bird
}
