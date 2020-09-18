package dynamicdata.domain

internal data class PersonWithGender(
    val name: String,
    val age: Int,
    val gender: String
) {
    constructor(person: Person, gender: String) : this(person.name, person.age, gender)
}
