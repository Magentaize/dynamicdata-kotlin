package xyz.magentaize.dynamicdata.domain

import xyz.magentaize.dynamicdata.kernel.NotifyPropertyChanged
import xyz.magentaize.dynamicdata.kernel.PropertyChangedDelegate

internal class Person(
    val name: String,
    age: Int,
    val gender: String = "F",
    val parentName: String = ""
) : NotifyPropertyChanged {
    companion object {
        fun age1Person(id: Int): Person =
            Person("Person${id}", 1)

        fun make100People(): List<Person> =
            (1..100).map(this::age1Person)

        fun make100AgedPeople(): List<Person> =
            (1..100).map { Person("Person$it", it) }
    }

    var age: Int by PropertyChangedDelegate(age)

    constructor(firstName: String, lastName: String, age: Int)
            : this("$firstName $lastName", age)

    override fun equals(other: Any?): Boolean {
        if (other !is Person)
            return false
        if (this === other)
            return true
        if (name == other.name && age == other.age && gender == other.gender)
            return true

        return false
    }

    override fun toString(): String {
        return "$name. $age"
    }

    private var _isDisposed = false

    override fun dispose() {
        super.dispose()
        _isDisposed = true
    }

    override fun isDisposed(): Boolean =
        _isDisposed
}
