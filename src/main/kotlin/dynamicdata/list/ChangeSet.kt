package dynamicdata.list

class ChangeSet<T>(items: Collection<Change<T>>): ArrayList<Change<T>>(items), IChangeSet<T> {
    companion object {
        private val INSTANCE = ChangeSet<Any>(emptyList())

        fun <T> empty(): IChangeSet<T> {
            return INSTANCE as IChangeSet<T>
        }
    }

    override val size: Int
        get() = super.size

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

    override fun toString(): String {
        return "ChangeSet<>. size=$size"
    }
}
