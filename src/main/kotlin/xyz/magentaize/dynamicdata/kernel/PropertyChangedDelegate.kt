package xyz.magentaize.dynamicdata.kernel

import kotlin.reflect.KProperty

class PropertyChangedDelegate<T>(initialValue: T) {
    private var value: T = initialValue

    operator fun getValue(thisRef: NotifyPropertyChanged, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(thisRef: NotifyPropertyChanged, property: KProperty<*>, value: T) {
        this.value = value

        thisRef.raisePropertyChanged(property.name)
    }
}