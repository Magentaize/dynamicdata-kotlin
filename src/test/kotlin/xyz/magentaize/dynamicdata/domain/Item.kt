package xyz.magentaize.dynamicdata.domain

import xyz.magentaize.dynamicdata.kernel.NotifyPropertyChanged
import xyz.magentaize.dynamicdata.kernel.PropertyChangedDelegate
import java.util.*

internal class Item(name: String) : NotifyPropertyChanged {
    val id: UUID = UUID.randomUUID()
    var name: String by PropertyChangedDelegate(name)
    private var _isDisposed = false

    override fun dispose() {
        super.dispose()
        _isDisposed = true
    }

    override fun isDisposed(): Boolean =
        _isDisposed
}
