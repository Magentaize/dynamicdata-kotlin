package xyz.magentaize.dynamicdata.kernel

import java.util.*

open class PropertyChangedEvent(
    sender: Any,
    val propertyName: String
) : EventObject(sender)
