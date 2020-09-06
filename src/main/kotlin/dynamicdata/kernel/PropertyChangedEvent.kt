package dynamicdata.kernel

import java.util.*

open class PropertyChangedEvent(
    sender: Any,
    open val propertyName: String
) : EventObject(sender)
