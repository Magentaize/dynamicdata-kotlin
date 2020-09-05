package dynamicdata.domain

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

internal data class Person(
    val name: String,
    val age: Int,
    val gender: String = "F",
    val parentName: String = ""):PropertyChangeListener {
    constructor(firstName: String, lastName: String, age: Int)
            : this("$firstName $lastName", age)

    override fun toString(): String {
        return "$name. $age"
    }

    override fun propertyChange(evt: PropertyChangeEvent?) {
        TODO("Not yet implemented")
    }
}
