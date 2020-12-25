package xyz.magentaize.dynamicdata.cache

class AnonymousChangeSet<K, V> : ArrayList<Change<K, V>>, ChangeSet<K, V> {
    companion object {
        private val INSTANCE: AnonymousChangeSet<Any?, Any?> = AnonymousChangeSet()

        fun <K, V> empty(): AnonymousChangeSet<K, V> {
            return INSTANCE as AnonymousChangeSet<K, V>
        }
    }

    constructor()
    constructor(collection: Collection<Change<K, V>>) : super(collection)
    constructor(capacity: Int) : super(capacity)

    override val size: Int
        get() = super.size

    override val adds
        get() =
            count(ChangeReason.Add)

    override val updates
        get() =
            count(ChangeReason.Update)

    override val removes
        get() =
            count(ChangeReason.Remove)

    override val refreshes
        get() =
            count(ChangeReason.Refresh)

    override val moves
        get() =
            count(ChangeReason.Moved)

    private fun count(reason: ChangeReason) =
        count { it.reason == reason }

    override fun toString(): String =
        "ChangeSet: TODO"

    private inline fun <reified TObject> to() =
        TObject::class.java
}
