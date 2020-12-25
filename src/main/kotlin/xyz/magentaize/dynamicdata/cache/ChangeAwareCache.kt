package xyz.magentaize.dynamicdata.cache

import xyz.magentaize.dynamicdata.kernel.Optional

class ChangeAwareCache<K, V>(
    private var changes: AnonymousChangeSet<K, V>,
    private var data: MutableMap<K, V>
) : ICache<K, V> {

    constructor() : this(AnonymousChangeSet(), LinkedHashMap())

    constructor(capacity: Int) : this(
        AnonymousChangeSet(capacity), LinkedHashMap(capacity)
    )

    constructor(data: Map<K, V>) : this(
        AnonymousChangeSet(), data.toMutableMap()
    )

    val size: Int
        get() = data.size

    override val keyValues: Map<K, V>
        get() = data.toMap()

    override val items: Collection<V>
        get() = data.values

    override val keys: Collection<K>
        get() = data.keys

    override fun lookup(key: K): Optional<V> {
        val d = data[key]
        return if (d == null)
            Optional.empty()
        else
            Optional.of(d)
    }

    fun add(item: V, key: K) {
        changes.add(Change(ChangeReason.Add, key, item))
        data[key] = item
    }

    override fun addOrUpdate(item: V, key: K) {
        val ex = data.contains(key)
        changes.add(
            if (ex) Change(ChangeReason.Update, key, item, Optional.ofNullable(data[key])) else Change(
                ChangeReason.Add,
                key,
                item
            )
        )
        data[key] = item
    }

    override fun remove(keys: Iterable<K>) {
        keys.forEach { remove(it) }
    }

    override fun remove(key: K) {
        val ex = data.remove(key)
        if (ex != null)
            changes.add(Change(ChangeReason.Remove, key, ex))
    }

    override fun refresh(keys: Iterable<K>) {
        keys.forEach { refresh(it) }
    }

    override fun refresh(key: K) {
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

    fun captureChanges(): ChangeSet<K, V> {
        if (changes.count() == 0) return AnonymousChangeSet.empty()

        val copy = changes
        changes = AnonymousChangeSet()
        return copy
    }

    override fun clone(changes: ChangeSet<K, V>) {
        TODO("Not yet implemented")
    }
}
