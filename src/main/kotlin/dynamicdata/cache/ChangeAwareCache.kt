package dynamicdata.cache

class ChangeAwareCache<TObject, TKey>(
    private var changes: ChangeSet<TObject, TKey>,
    private var data: MutableMap<TKey, TObject>
) : ICache<TObject, TKey> {

    constructor() : this(ChangeSet(), LinkedHashMap())

    constructor(capacity: Int) : this(
        ChangeSet(capacity), LinkedHashMap(capacity)
    )

    constructor(data: Map<TKey, TObject>) : this(
        ChangeSet(), data.toMutableMap()
    )

    val size: Int
        get() = data.size

    override val keyValues: Map<TKey, TObject>
        get() = data.toMap()

    override val items: Collection<TObject>
        get() = data.values

    override val keys: Collection<TKey>
        get() = data.keys

    override fun lookup(key: TKey): TObject? {
        return data.getOrDefault(key, null)
    }

    fun add(item: TObject, key: TKey) {
        changes.add(Change(ChangeReason.Add, key, item))
        data[key] = item
    }

    override fun addOrUpdate(item: TObject, key: TKey) {
        val ex = data.contains(key)
        changes.add(
            if (ex) Change(ChangeReason.Update, key, item, data[key]) else Change(
                ChangeReason.Add,
                key,
                item
            )
        )
        data[key] = item
    }

    override fun remove(keys: Iterable<TKey>) {
        keys.forEach { remove(it) }
    }

    override fun remove(key: TKey) {
        val ex = data.remove(key)
        if (ex != null)
            changes.add(Change(ChangeReason.Remove, key, ex))
    }

    override fun refresh(keys: Iterable<TKey>) {
        keys.forEach { refresh(it) }
    }

    override fun refresh(key: TKey) {
        val ex = data[key]
        if (ex != null)
            changes.add(Change(ChangeReason.Refresh, key, ex))
    }

    override fun refresh() {
        changes.addAll(data.map { Change(ChangeReason.Refresh, it.key, it.value) })
    }

    override fun clear() {
        val toRemove = data.map { Change(ChangeReason.Remove, it.key, it.value) }
        changes.addAll(toRemove)
        data.clear()
    }

    fun captureChanges(): IChangeSet<TObject, TKey> {
        if (changes.count() == 0) return ChangeSet.empty()

        val copy = changes
        changes = ChangeSet()
        return copy
    }

    override fun clone(changes: ChangeSet<TObject, TKey>) {
        TODO("Not yet implemented")
    }
}
