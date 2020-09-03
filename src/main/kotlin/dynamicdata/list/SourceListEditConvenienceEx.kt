package dynamicdata.list

fun <T> ISourceList<T>.add(item: T) =
    this.edit { it.add(item) }

fun <T> ISourceList<T>.clear() =
    this.edit { it.clear() }
