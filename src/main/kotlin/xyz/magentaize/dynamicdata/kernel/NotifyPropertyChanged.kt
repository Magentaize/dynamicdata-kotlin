package xyz.magentaize.dynamicdata.kernel

import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject

interface NotifyPropertyChanged : Disposable {
    val propertyChanged: Subject<PropertyChangedEvent>
        get() = eventHandlers.getOrPut(hashCode(), { PublishSubject.create() })

    fun raisePropertyChanged(propertyName: String = "") {
        propertyChanged.onNext(PropertyChangedEvent(this, propertyName))
    }

    override fun dispose() {
        propertyChanged.onComplete()
        eventHandlers.remove(hashCode())
    }

    companion object {
        private val eventHandlers: MutableMap<Int, Subject<PropertyChangedEvent>> = mutableMapOf()
    }
}
