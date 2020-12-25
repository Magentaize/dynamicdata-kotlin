package xyz.magentaize.dynamicdata.cache

class CacheChangeSetEx {
    /// IChangeSet is flawed because it automatically means allocations when enumerating.
    /// This extension is a crazy hack to cast to the concrete change set which means we no longer allocate
    /// as  change set now inherits from List which has allocation free enumerations.
    ///
    /// IChangeSet will be removed in V7 and instead Change sets will be used directly
    ///
    /// In the mean time I am banking that no-one has implemented a custom change set - personally I think it is very unlikely.
    fun <K, V> ChangeSet<K, V>.toConcreteType(): AnonymousChangeSet<K, V> =
        this as AnonymousChangeSet<K, V>
}