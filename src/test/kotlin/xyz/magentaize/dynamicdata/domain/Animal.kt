package xyz.magentaize.dynamicdata.domain

import xyz.magentaize.dynamicdata.kernel.NotifyPropertyChanged
import xyz.magentaize.dynamicdata.kernel.PropertyChangedDelegate

class Animal(
    val name: String,
    val type: String,
    val family: AnimalFamily
) : NotifyPropertyChanged {
    var includeInResults: Boolean by PropertyChangedDelegate(false)

    private var _isDisposed = false

    override fun dispose() {
        super.dispose()
        _isDisposed = true
    }

    override fun isDisposed(): Boolean =
        _isDisposed
}

enum class AnimalFamily {
    Mammal,
    Reptile,
    Fish,
    Amphibian,
    Bird
}
