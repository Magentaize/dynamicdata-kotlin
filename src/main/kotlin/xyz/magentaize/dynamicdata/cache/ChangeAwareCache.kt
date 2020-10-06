package xyz.magentaize.dynamicdata.cache

import xyz.magentaize.dynamicdata.kernel.Optional

class ChangeAwareCache<T, TKey>(
    private var changes: AnonymousChangeSet<T, TKey>,
    private var data: MutableMap<TKey, T>
) : ICache<T, TKey> {

    constructor() : this(AnonymousChangeSet(), LinkedHashMap())

    constructor(capacity: Int) : this(
        AnonymousChangeSet(capacity), LinkedHashMap(capacity)
    )

    constructor(data: Map<TKey, T>) : this(
        AnonymousChangeSet(), data.toMutableMap()
    )

    val size: Int
        get() = data.size

    override val keyValues: Map<TKey, T>
        get() = data.toMap()

    override val items: Collection<T>
        get() = data.values

    override val keys: Collection<TKey>
        get() = data.keys

    override fun lookup(key: TKey): Optional<T> {
        val d = data[key]
        return if (d == null)
            Optional.empty()
        else
            Optional.of(d)
    }

    fun add(item: T, key: TKey) {
        changes.add(Change(ChangeReason.Add, key, item))
        data[key] = item
    }

    override fun addOrUpdate(item: T, key: TKey) {
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

    fun captureChanges(): ChangeSet<T, TKey> {
        if (changes.count() == 0) return AnonymousChangeSet.empty()

        val copy = changes
        changes = AnonymousChangeSet()
        return copy
    }

    override fun clone(changes: ChangeSet<T, TKey>) {
        TODO("Not yet implemented")
    }
}
