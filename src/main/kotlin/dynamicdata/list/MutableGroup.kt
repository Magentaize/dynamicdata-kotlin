package dynamicdata.list

interface MutableGroup<T, out K> {
    val key: K
    val list: IObservableList<T>
}
