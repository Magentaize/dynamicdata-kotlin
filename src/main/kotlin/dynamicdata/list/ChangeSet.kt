package dynamicdata.list

class ChangeSet<T> : ArrayList<Change<T>>, IChangeSet<T> {
    constructor()
    constructor(items: Collection<Change<T>>) : super(items)

    override val adds: Int
        get() =
            sumBy {
                return@sumBy when (it.reason) {
                    ListChangeReason.Add -> return 1
                    ListChangeReason.AddRange -> it.range.size
                    else -> 0
                }

            }

    override val replaced: Int
        get() = count { it.reason == ListChangeReason.Replace }

    override val removes: Int
        get() = sumBy {
            return@sumBy when (it.reason) {
                ListChangeReason.Remove -> 1
                in setOf(ListChangeReason.RemoveRange, ListChangeReason.Clear) -> it.range.size
                else -> 0
            }
        }

    override val refreshes: Int
        get() = count { it.reason == ListChangeReason.Refresh }

    override val moves: Int
        get() = count { it.reason == ListChangeReason.Moved }

    override val totalChanges: Int
        get() = adds + removes + replaced + moves
}
