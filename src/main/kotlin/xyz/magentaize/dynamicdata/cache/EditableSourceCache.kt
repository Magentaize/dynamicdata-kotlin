package xyz.magentaize.dynamicdata.cache

interface EditableSourceCache<K, V> : ObservableCache<K, V> {
    val keySelector: (V) -> K
    fun edit(updateAction: (ISourceUpdater<K, V>) -> Unit)
}