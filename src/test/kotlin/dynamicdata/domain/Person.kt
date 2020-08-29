package dynamicdata.domain

internal data class Person(val name: String, val age: Int, val gender: String = "F", val parentName: String = "") {
    constructor(firstName: String, lastName: String, age: Int)
            : this("$firstName $lastName", age)
}
