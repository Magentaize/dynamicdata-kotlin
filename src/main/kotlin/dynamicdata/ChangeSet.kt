package dynamicdata

interface ChangeSet {
    val adds: Int
    val removes: Int
    val refreshes: Int
    val moves: Int
    //fun size(): Int
    //val capacity: Int
}
