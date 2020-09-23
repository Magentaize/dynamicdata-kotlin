package xyz.magentaize.dynamicdata.domain

class PersonWithRelations(
    val name: String,
    val age: Int,
    val keyValue: String,
    val relations: Iterable<PersonWithRelations>
) {
    constructor(
        name: String,
        age: Int
    ) : this(
        name, age, listOf(),
    )

    constructor(
        name: String,
        age: Int,
        relations: Iterable<PersonWithRelations>
    ) : this(
        name,
        age,
        name,
        relations,
    )

    override fun toString(): String {
        return "$name. $age"
    }
}
