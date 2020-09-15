package dynamicdata.domain

import java.util.*
import kotlin.random.Random

internal class RandomPersonGenerator {
    companion object {
        private val _boys = listOf(
            "Sergio", "Daniel", "Carolina", "David", "Reina", "Saul", "Bernard", "Danny",
            "Dimas", "Yuri", "Ivan", "Laura", "John", "Bob", "Charles", "Rupert", "William",
            "Englebert", "Aaron", "Quasimodo", "Henry", "Edward", "Zak",
            "Kai", "Dominguez", "Escobar", "Martin", "Crespo", "Xavier", "Lyons", "Stephens", "Aaron"
        )

        private val _girls = listOf(
            "Johnson", "Williams", "Jones", "Brown", "David", "Miller", "Wilson", "Anderson", "Thomas",
            "Jackson", "White", "Robinson", "Williams", "Jones", "Windor", "McQueen", "X", "Black",
            "Green", "Chicken", "Partrige", "Broad", "Flintoff", "Root"
        )

        private val _lastnames = listOf(
            "Johnson", "Williams", "Jones", "Brown", "David", "Miller", "Wilson", "Anderson", "Thomas",
            "Jackson", "White", "Robinson", "Williams", "Jones", "Windor", "McQueen", "X", "Black",
            "Green", "Chicken", "Partrige", "Broad", "Flintoff", "Root"
        )

        fun take(number: Int = 10000): List<Person> {
            val girls = _girls.flatMap { first ->
                _lastnames.flatMap { second ->
                    _lastnames.map { third ->
                        Triple(first, second, third)
                    }
                }
            }

            val boys = _boys.flatMap { first ->
                _lastnames.flatMap { second ->
                    _lastnames.map { third ->
                        Triple(first, second, third)
                    }
                }
            }

            val maxAge = 100

            return girls.union(boys)
                .sortedBy { UUID.randomUUID() }
                .map {
                    val lastname = if (it.second == it.third)
                        it.second
                    else
                        "${it.second}-${it.third}"
                    val age = Random.nextInt(0, maxAge)
                    Person(it.first, lastname, age)
                }
                .take(number)
                .toList()
        }
    }
}
